package com.granatum.feature.auth.presentation.login

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.util.Result
import com.granatum.feature.auth.presentation.testing.FakeAuthRepository
import com.granatum.feature.auth.presentation.testing.FakeEnvironmentRepository
import com.granatum.core.domain.auth.model.DemoAccount
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.environment.AppEnvironment
import com.granatum.core.domain.environment.EnvironmentError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val repository = FakeAuthRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(email: String = "ana@granatum.es", password: String = "secreta") =
        LoginViewModel(repository, FakeEnvironmentRepository()).apply {
            emailState.setTextAndPlaceCursorAtEnd(email)
            passwordState.setTextAndPlaceCursorAtEnd(password)
            Snapshot.sendApplyNotifications()
        }

    @Test
    fun empty_fields_are_flagged_without_calling_the_server() = runTest {
        val vm = viewModel(email = "", password = "")
        vm.onAction(LoginAction.OnSubmit)
        assertEquals(EmailFieldError.REQUIRED, vm.state.value.emailError)
        assertEquals(PasswordFieldError.REQUIRED, vm.state.value.passwordError)
        assertTrue(repository.loginCalls.isEmpty())
    }

    @Test
    fun a_malformed_email_is_flagged() = runTest {
        val vm = viewModel(email = "ana@granatum")
        vm.onAction(LoginAction.OnSubmit)
        assertEquals(EmailFieldError.INVALID, vm.state.value.emailError)
        assertTrue(repository.loginCalls.isEmpty())
    }

    @Test
    fun lengths_beyond_the_contract_are_flagged() {
        assertEquals(EmailFieldError.TOO_LONG, LoginViewModel.validateEmail("a".repeat(250) + "@b.es"))
        assertEquals(PasswordFieldError.TOO_LONG, LoginViewModel.validatePassword("x".repeat(129)))
        assertNull(LoginViewModel.validatePassword("x".repeat(128)))
    }

    @Test
    fun a_second_tap_while_signing_in_sends_nothing() = runTest {
        repository.gate = CompletableDeferred()
        val vm = viewModel()

        vm.onAction(LoginAction.OnSubmit)
        assertTrue(vm.state.value.isLoading)
        vm.onAction(LoginAction.OnSubmit)
        repository.gate!!.complete(Unit)

        assertEquals(1, repository.loginCalls.size)
        assertEquals(false, vm.state.value.isLoading)
    }

    @Test
    fun the_email_is_trimmed_and_the_password_sent_as_typed() = runTest {
        viewModel(email = "  ana@granatum.es ", password = " a b ").onAction(LoginAction.OnSubmit)
        assertEquals("ana@granatum.es" to " a b ", repository.loginCalls.single())
    }

    @Test
    fun a_server_error_is_shown_and_cleared_on_edit() = runTest {
        repository.nextResult = Result.Failure(AuthError.TooManyAttempts(30))
        val vm = viewModel()
        vm.onAction(LoginAction.OnSubmit)
        assertEquals(AuthError.TooManyAttempts(30), vm.state.value.error)

        vm.passwordState.setTextAndPlaceCursorAtEnd("otra")
        Snapshot.sendApplyNotifications()
        assertNull(vm.state.value.error)
    }

    @Test
    fun the_store_app_shows_no_environment_selector() = runTest {
        val sut = LoginViewModel(repository, FakeEnvironmentRepository(available = listOf(AppEnvironment.PRO)))
        assertTrue(sut.state.value.environments.isEmpty())
        assertTrue(sut.state.value.demoAccounts.isEmpty())
    }

    @Test
    fun when_demo_is_picked_then_its_accounts_sign_in_in_one_tap() = runTest {
        val admin = DemoAccount(UserRole.ADMIN, "Lucía", "admin@demo.granatum.es", "Demo-Admin-2026!")
        val environments = FakeEnvironmentRepository(
            available = listOf(AppEnvironment.PRE, AppEnvironment.PRO, AppEnvironment.DEMO),
            demo = listOf(admin)
        )
        val sut = LoginViewModel(repository, environments)
        assertEquals(listOf(AppEnvironment.PRE, AppEnvironment.PRO, AppEnvironment.DEMO), sut.state.value.environments)

        sut.onAction(LoginAction.OnEnvironment(AppEnvironment.DEMO))
        assertEquals(AppEnvironment.DEMO, sut.state.value.environment)
        assertEquals(listOf(admin), sut.state.value.demoAccounts)

        sut.onAction(LoginAction.OnDemoAccount(admin))
        assertEquals(listOf("admin@demo.granatum.es" to "Demo-Admin-2026!"), repository.loginCalls)
    }

    @Test
    fun when_punches_are_pending_then_the_switch_is_refused_and_explained() = runTest {
        val environments = FakeEnvironmentRepository(available = listOf(AppEnvironment.PRE, AppEnvironment.PRO))
        environments.refuseWith = EnvironmentError.UnsyncedWork(2)
        val sut = LoginViewModel(repository, environments)

        sut.onAction(LoginAction.OnEnvironment(AppEnvironment.PRO))

        assertEquals(AppEnvironment.PRE, sut.state.value.environment)
        assertEquals(EnvironmentError.UnsyncedWork(2), sut.state.value.environmentError)
    }
}
