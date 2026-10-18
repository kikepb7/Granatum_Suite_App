package com.granatum.feature.auth.presentation.signup

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.OwnerRegistration
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.validation.PasswordRequirement
import com.granatum.feature.auth.presentation.login.EmailFieldError
import com.granatum.feature.auth.presentation.testing.FakeAuthRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerSignUpViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeAuthRepository()

    private fun OwnerSignUpViewModel.fill(password: String = "Clave-2026!") {
        name.setTextAndPlaceCursorAtEnd("Ana")
        lastName.setTextAndPlaceCursorAtEnd(" Martín Soler ")
        document.setTextAndPlaceCursorAtEnd("12345678Z")
        email.setTextAndPlaceCursorAtEnd(" ana@granatum.es ")
        this.password.setTextAndPlaceCursorAtEnd(password)
        code.setTextAndPlaceCursorAtEnd("codigo-de-arranque-de-prueba")
    }

    @Test
    fun an_empty_form_is_flagged_and_not_sent() = runTest {
        val vm = OwnerSignUpViewModel(repo)
        vm.onAction(OwnerSignUpAction.OnSubmit)

        val state = vm.state.value
        assertEquals(setOf(SignUpField.NAME, SignUpField.LAST_NAME, SignUpField.DOCUMENT, SignUpField.CODE, SignUpField.PASSWORD), state.problems.keys)
        assertEquals(EmailFieldError.REQUIRED, state.emailError)
        assertTrue(repo.registrations.isEmpty())
    }

    @Test
    fun blank_or_too_long_last_names_are_not_sent() = runTest {
        val vm = OwnerSignUpViewModel(repo)
        vm.fill()
        vm.lastName.setTextAndPlaceCursorAtEnd("   ")
        vm.onAction(OwnerSignUpAction.OnSubmit)
        assertEquals(mapOf(SignUpField.LAST_NAME to FieldProblem.REQUIRED), vm.state.value.problems)

        vm.lastName.setTextAndPlaceCursorAtEnd("G".repeat(OwnerRegistration.MAX_LAST_NAME + 1))
        vm.onAction(OwnerSignUpAction.OnSubmit)
        assertEquals(mapOf(SignUpField.LAST_NAME to FieldProblem.TOO_LONG), vm.state.value.problems)
        assertTrue(repo.registrations.isEmpty())
    }

    @Test
    fun a_weak_password_is_caught_before_sending() = runTest {
        val vm = OwnerSignUpViewModel(repo)
        vm.fill(password = "corta")
        vm.onAction(OwnerSignUpAction.OnSubmit)

        assertTrue(PasswordRequirement.MIN_LENGTH in vm.state.value.unmetRequirements)
        assertTrue(repo.registrations.isEmpty())
    }

    @Test
    fun a_complete_form_registers_the_owner() = runTest {
        val vm = OwnerSignUpViewModel(repo)
        vm.fill()
        vm.onAction(OwnerSignUpAction.OnSubmit)

        val sent = repo.registrations.single()
        assertEquals("ana@granatum.es", sent.email)
        assertEquals("Ana", sent.name)
        assertEquals("Martín Soler", sent.lastName)
        assertEquals("codigo-de-arranque-de-prueba", sent.bootstrapCode)
        assertEquals("Clave-2026!", sent.password)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun server_errors_land_on_their_field_or_the_banner() = runTest {
        val vm = OwnerSignUpViewModel(repo)
        vm.fill()

        repo.nextResult = Result.Failure(AuthError.EmailTaken)
        vm.onAction(OwnerSignUpAction.OnSubmit)
        assertEquals(FieldProblem.TAKEN, vm.state.value.problems[SignUpField.EMAIL])

        repo.nextResult = Result.Failure(AuthError.InvalidDocument)
        vm.onAction(OwnerSignUpAction.OnSubmit)
        assertEquals(FieldProblem.INVALID, vm.state.value.problems[SignUpField.DOCUMENT])

        repo.nextResult = Result.Failure(AuthError.WeakPassword(setOf(PasswordRequirement.MISSING_SYMBOL)))
        vm.onAction(OwnerSignUpAction.OnSubmit)
        assertEquals(setOf(PasswordRequirement.MISSING_SYMBOL), vm.state.value.serverUnmetRequirements)

        repo.nextResult = Result.Failure(AuthError.InvalidBootstrapCode)
        vm.onAction(OwnerSignUpAction.OnSubmit)
        assertEquals(AuthError.InvalidBootstrapCode, vm.state.value.error)
    }
}
