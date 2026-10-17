package com.granatum.core.data.environment

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.granatum.core.data.testing.RecordingLogger
import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.DemoAccount
import com.granatum.core.domain.auth.model.OwnerRegistration
import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.environment.AppEnvironment
import com.granatum.core.domain.environment.AppEnvironment.DEMO
import com.granatum.core.domain.environment.AppEnvironment.LOCAL
import com.granatum.core.domain.environment.AppEnvironment.PRE
import com.granatum.core.domain.environment.AppEnvironment.PRO
import com.granatum.core.domain.environment.EnvironmentError
import com.granatum.core.domain.environment.EnvironmentScopedData
import com.granatum.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** specs/010-entornos: which environments exist, and switching without mixing data. */
class EnvironmentTest {

    private class MemoryDataStore : DataStore<Preferences> {
        private val flow = MutableStateFlow(emptyPreferences())
        override val data = flow
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(flow.value).also { flow.value = it }
    }

    private class RecordingAuth : AuthRepository {
        val calls = mutableListOf<String>()
        override suspend fun login(email: String, password: String): Result<Session, AuthError> {
            calls += "login:$email"
            return Result.Failure(AuthError.Unknown)
        }
        override suspend fun registerOwner(registration: OwnerRegistration): Result<Session, AuthError> = Result.Failure(AuthError.Unknown)
        override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Session, AuthError> = Result.Failure(AuthError.Unknown)
        override suspend fun logout() {
            calls += "logout:${ActiveEnvironment.current}"
        }
    }

    private class Scoped(var pending: Int = 0) : EnvironmentScopedData {
        var cleared = 0
        override suspend fun unsyncedItems() = pending
        override suspend fun clear() {
            cleared++
        }
    }

    private val demoAdmin = DemoAccount(UserRole.ADMIN, "Lucía", "admin@demo", "x")

    private class Gateway(override val defaultAccount: DemoAccount) : DemoGateway {
        override fun engine(): HttpClientEngine = MockEngine { respond("") }
        override val accounts = listOf(defaultAccount)
    }

    private val store = MemoryDataStore()
    private val auth = RecordingAuth()
    private val clockIn = Scoped()
    private val inventory = Scoped()

    private fun repository(available: List<AppEnvironment> = listOf(LOCAL, PRE, PRO, DEMO)) = EnvironmentRepositoryImpl(
        dataStore = store,
        authRepository = { auth },
        scopedData = { listOf(clockIn, inventory) },
        demo = Gateway(demoAdmin),
        logger = RecordingLogger(),
        available = available
    )

    @AfterTest
    fun reset() {
        ActiveEnvironment.current = LOCAL
    }

    @Test
    fun the_store_app_only_has_production_and_the_internal_one_what_is_configured() {
        assertEquals(listOf(PRO), ActiveEnvironment.availableFor(proBuild = true, demoAvailable = true) { true })
        assertEquals(listOf(LOCAL, PRE, PRO, DEMO), ActiveEnvironment.availableFor(proBuild = false, demoAvailable = true) { true })
        assertEquals(listOf(LOCAL, PRO), ActiveEnvironment.availableFor(proBuild = false, demoAvailable = false) { it != PRE })
        assertEquals(PRO, ActiveEnvironment.default(listOf(PRO)))
        assertEquals(PRE, ActiveEnvironment.default(listOf(LOCAL, PRE, PRO)))
        assertEquals(LOCAL, ActiveEnvironment.default(listOf(LOCAL, PRO)))
    }

    @Test
    fun a_fresh_install_starts_on_pre_and_a_choice_is_remembered() = runTest {
        val first = repository()
        first.load()
        assertEquals(PRE, first.current.value)
        assertEquals(PRE, ActiveEnvironment.current)

        first.switchTo(PRO)
        val reopened = repository()
        reopened.load()
        assertEquals(PRO, reopened.current.value)
        assertEquals(PRO, ActiveEnvironment.current)
    }

    @Test
    fun switching_signs_out_against_the_old_server_and_clears_every_feature() = runTest {
        val sut = repository()
        sut.load()

        val actual = sut.switchTo(PRO)

        assertEquals(Result.Success(Unit), actual)
        assertEquals(listOf("logout:PRE"), auth.calls)
        assertEquals(1 to 1, clockIn.cleared to inventory.cleared)
        assertEquals(PRO, sut.current.value)
    }

    @Test
    fun unsent_punches_block_the_switch() = runTest {
        val sut = repository()
        sut.load()
        clockIn.pending = 3

        val actual = sut.switchTo(PRO)

        assertEquals(Result.Failure(EnvironmentError.UnsyncedWork(3)), actual)
        assertEquals(PRE, sut.current.value)
        assertTrue(auth.calls.isEmpty())
        assertEquals(0, clockIn.cleared)
    }

    @Test
    fun demo_signs_in_as_the_administrator_and_offers_its_accounts() = runTest {
        val sut = repository()
        sut.load()
        assertEquals(emptyList(), sut.demoAccounts())

        sut.switchTo(DEMO)

        assertEquals(listOf("logout:PRE", "login:admin@demo"), auth.calls)
        assertEquals(listOf(demoAdmin), sut.demoAccounts())
    }

    @Test
    fun an_environment_this_build_does_not_have_is_refused() = runTest {
        val sut = repository(available = listOf(PRO))
        sut.load()
        assertEquals(Result.Failure(EnvironmentError.NotAvailable), sut.switchTo(PRE))
        assertEquals(false, sut.canSwitch)
    }

    @Test
    fun starting_in_demo_without_a_session_signs_in_and_releases_the_splash() = runTest {
        repository().apply { load(); switchTo(DEMO) }
        auth.calls.clear()
        val sut = repository()
        sut.load()
        assertEquals(true, sut.startingDemo.value)

        sut.finishStartup(hasSession = false)

        assertEquals(listOf("login:admin@demo"), auth.calls)
        assertEquals(false, sut.startingDemo.value)
    }

    @Test
    fun the_switching_engine_sends_demo_requests_to_the_demo_backend_only() = runTest {
        var environment = PRE
        val engine = SwitchingEngine(
            real = MockEngine { respond("real") },
            demo = MockEngine { respond("demo") },
            environment = { environment }
        )
        val client = HttpClient(engine)

        assertEquals("real", client.get("https://x/api/a").bodyAsText())
        environment = DEMO
        assertEquals("demo", client.get("https://x/api/a").bodyAsText())
        environment = PRO
        assertEquals("real", client.get("https://x/api/a").bodyAsText())
    }

    @Test
    fun routes_follow_the_active_environment() = runTest {
        ActiveEnvironment.current = DEMO
        assertEquals(ActiveEnvironment.DEMO_BASE_URL, com.granatum.core.data.networking.UrlConstants.BASE_URL_HTTP)
        assertEquals("${ActiveEnvironment.DEMO_BASE_URL}/materials", com.granatum.core.data.networking.constructRoute("/materials"))
    }
}
