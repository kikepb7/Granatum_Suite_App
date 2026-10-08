package com.granatum.feature.auth.presentation.password

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.validation.PasswordRequirement
import com.granatum.feature.auth.presentation.testing.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {

    private val repository = FakeAuthRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(current: String = "Temp0ral!", new: String = "Granatum1!", repeat: String = new) =
        ChangePasswordViewModel(repository, ChangePasswordMode.MANDATORY).apply {
            currentState.setTextAndPlaceCursorAtEnd(current)
            newState.setTextAndPlaceCursorAtEnd(new)
            repeatState.setTextAndPlaceCursorAtEnd(repeat)
            Snapshot.sendApplyNotifications()
        }

    @Test
    fun requirements_are_recomputed_while_typing() = runTest {
        val vm = viewModel(new = "granatum")
        assertEquals(
            setOf(PasswordRequirement.FALTA_MAYUSCULA, PasswordRequirement.FALTA_DIGITO, PasswordRequirement.FALTA_SIMBOLO),
            vm.state.value.unmetRequirements
        )
        vm.newState.setTextAndPlaceCursorAtEnd("Granatum1!")
        Snapshot.sendApplyNotifications()
        assertTrue(vm.state.value.unmetRequirements.isEmpty())
    }

    @Test
    fun nothing_is_sent_while_requirements_are_unmet_or_the_repeat_differs() = runTest {
        viewModel(new = "corta").onAction(ChangePasswordAction.OnSubmit)
        val mismatch = viewModel(repeat = "Granatum1?")
        mismatch.onAction(ChangePasswordAction.OnSubmit)
        assertTrue(mismatch.state.value.repeatMismatch)
        assertTrue(repository.changeCalls.isEmpty())
    }

    @Test
    fun a_wrong_current_password_keeps_the_new_one() = runTest {
        repository.nextResult = Result.Failure(AuthError.InvalidCredentials)
        val vm = viewModel()
        vm.onAction(ChangePasswordAction.OnSubmit)
        assertTrue(vm.state.value.currentPasswordWrong)
        assertEquals("Granatum1!", vm.newState.text.toString())
        assertEquals("Granatum1!", vm.repeatState.text.toString())
    }

    @Test
    fun the_servers_requirements_are_shown_exactly() = runTest {
        repository.nextResult = Result.Failure(AuthError.WeakPassword(setOf(PasswordRequirement.FALTA_SIMBOLO)))
        val vm = viewModel()
        vm.onAction(ChangePasswordAction.OnSubmit)
        assertEquals(setOf(PasswordRequirement.FALTA_SIMBOLO), vm.state.value.serverUnmetRequirements)
    }

    @Test
    fun a_double_tap_sends_once() = runTest {
        repository.gate = CompletableDeferred()
        val vm = viewModel()
        vm.onAction(ChangePasswordAction.OnSubmit)
        vm.onAction(ChangePasswordAction.OnSubmit)
        repository.gate!!.complete(Unit)
        assertEquals(1, repository.changeCalls.size)
    }

    @Test
    fun success_emits_changed() = runTest {
        val vm = viewModel()
        vm.onAction(ChangePasswordAction.OnSubmit)
        assertEquals(ChangePasswordEvent.Changed, vm.events.first())
        assertEquals("Temp0ral!" to "Granatum1!", repository.changeCalls.single())
    }
}
