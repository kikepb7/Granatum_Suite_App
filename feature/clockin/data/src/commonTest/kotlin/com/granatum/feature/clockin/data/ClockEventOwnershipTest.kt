package com.granatum.feature.clockin.data

import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.logger.AppLogger
import com.granatum.feature.clockin.data.datasource.local.OfflineFirstClockInRepositoryImpl
import com.granatum.feature.clockin.data.sync.ClockEventSyncManager
import com.granatum.feature.clockin.database.dao.ClockEventDao
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Spec 004, FR-028 / SC-008: a punch is sent only under the session of the person who made it.
 * The server attributes a punch to whoever signs the request, so sending person A's queue with
 * person B's session would put A's working hours on B's record.
 */
class ClockEventOwnershipTest {

    private class InMemoryDao : ClockEventDao {
        val rows = MutableStateFlow<List<ClockEventEntity>>(emptyList())
        private fun mine(employeeId: String) = rows.value.filter { it.employeeId == employeeId }
        private fun pending(e: ClockEventEntity) = e.syncState == "PENDING" || e.syncState == "FAILED"

        override fun observeAllEvents(employeeId: String) = rows.map { all -> all.filter { it.employeeId == employeeId } }
        override fun observeEventsBetween(employeeId: String, fromEpochMillis: Long, toEpochMillis: Long) =
            rows.map { all -> all.filter { it.employeeId == employeeId && it.clientTimestampEpochMillis in fromEpochMillis..toEpochMillis } }
        override suspend fun getLatestEvent(employeeId: String) = mine(employeeId).maxByOrNull { it.clientTimestampEpochMillis }
        override fun observeLatestEvent(employeeId: String) =
            rows.map { all -> all.filter { it.employeeId == employeeId }.maxByOrNull { it.clientTimestampEpochMillis } }
        override fun observePendingCount(employeeId: String) = rows.map { all -> all.count { it.employeeId == employeeId && pending(it) } }
        override suspend fun getPendingEvents(employeeId: String) = mine(employeeId).filter(::pending).sortedBy { it.clientTimestampEpochMillis }
        override suspend fun upsertEvent(event: ClockEventEntity) { rows.value = rows.value.filterNot { it.id == event.id } + event }
        override suspend fun markSynced(id: String, serverTimestampEpochMillis: Long) = update(id) { it.copy(syncState = "SYNCED", serverTimestampEpochMillis = serverTimestampEpochMillis) }
        override suspend fun markFailed(id: String, attemptEpochMillis: Long) = update(id) { it.copy(syncState = "FAILED") }
        override suspend fun markSyncing(id: String) = update(id) { it.copy(syncState = "SYNCING") }
        private fun update(id: String, change: (ClockEventEntity) -> ClockEventEntity) {
            rows.value = rows.value.map { if (it.id == id) change(it) else it }
        }
    }

    private class FakeSessions : SessionStorage {
        val session = MutableStateFlow<Session?>(null)
        override fun observeSession(): Flow<Session?> = session
        override suspend fun set(session: Session?) { this.session.value = session }
        fun signIn(employeeId: String) {
            session.value = Session("a", "r", employeeId, UserRole.EMPLEADO, false, "$employeeId@granatum.es")
        }
    }

    private object SilentLogger : AppLogger {
        override fun debug(message: String) = Unit
        override fun info(message: String) = Unit
        override fun warn(message: String) = Unit
        override fun error(message: String, throwable: Throwable?) = Unit
    }

    private val dao = InMemoryDao()
    private val sessions = FakeSessions()
    private var online = true
    private val sent = mutableListOf<String>()

    private val client = HttpClient(MockEngine { request: HttpRequestData ->
        if (!online) throw kotlinx.io.IOException("offline")
        val body = request.body.toByteArray().decodeToString()
        val id = Regex("\"id\":\"([^\"]+)\"").find(body)!!.groupValues[1]
        sent += id
        respond(
            content = """{"id":"$id","type":"CLOCK_IN","clientTimestamp":"2026-10-08T08:00:00Z","serverTimestamp":"2026-10-08T08:00:01Z"}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }) {
        install(ContentNegotiation) { json() }
        // As the app's client does (HttpClientFactory); without it the body is not serialised.
        defaultRequest { contentType(ContentType.Application.Json) }
    }

    private val syncManager = ClockEventSyncManager(client, dao, sessions, flowOf(true), SilentLogger)

    private fun TestScope.repository() = OfflineFirstClockInRepositoryImpl(
        httpClient = client, dao = dao, sessionStorage = sessions, syncManager = syncManager, appScope = backgroundScope
    )

    @Test
    fun a_new_punch_carries_the_signed_in_person() = runTest {
        sessions.signIn("ana")
        online = false
        repository().clockIn()
        assertEquals(listOf("ana"), dao.rows.value.map { it.employeeId })
    }

    @Test
    fun without_a_session_nothing_can_be_punched() = runTest {
        assertEquals(Result.Failure(ClockActionError.NoSession), repository().clockIn())
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun another_persons_pending_punches_are_neither_sent_nor_shown() = runTest {
        val repository = repository()
        sessions.signIn("ana")
        online = false
        repository.clockIn()
        val anasPunch = dao.rows.value.single().id

        sessions.signIn("bea")
        online = true
        syncManager.syncNow()

        assertTrue(anasPunch !in sent, "Ana's punch was sent under Bea's session")
        assertEquals(0, repository.observePendingSyncCount().first())
        assertTrue(repository.observeTodayEvents().first().isEmpty())

        sessions.signIn("ana")
        syncManager.syncNow()
        assertEquals(listOf(anasPunch), sent)
    }

    @Test
    fun rows_without_an_owner_are_never_sent() = runTest {
        dao.upsertEvent(ClockEventEntity("legacy", "CLOCK_IN", 1L, null, "PENDING", 0, null, employeeId = null))
        sessions.signIn("ana")
        syncManager.syncNow()
        assertTrue(sent.isEmpty())
    }
}
