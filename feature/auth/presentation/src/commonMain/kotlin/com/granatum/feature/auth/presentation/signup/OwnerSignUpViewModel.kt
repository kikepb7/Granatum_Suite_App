package com.granatum.feature.auth.presentation.signup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.OwnerRegistration
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.validation.PasswordRequirement
import com.granatum.core.domain.validation.PasswordValidator
import com.granatum.feature.auth.presentation.login.EmailFieldError
import com.granatum.feature.auth.presentation.login.LoginViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SignUpField { NAME, DOCUMENT, EMAIL, PASSWORD, CODE }

enum class FieldProblem { REQUIRED, TOO_LONG, INVALID, TAKEN }

data class OwnerSignUpState(
    val problems: Map<SignUpField, FieldProblem> = emptyMap(),
    val emailError: EmailFieldError? = null,
    val unmetRequirements: Set<PasswordRequirement> = PasswordValidator.validate(""),
    val serverUnmetRequirements: Set<PasswordRequirement> = emptySet(),
    /** Errors that belong to no single field (wrong code, no coverage…), shown as a banner. */
    val error: AuthError? = null,
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false
)

sealed interface OwnerSignUpAction {
    data object OnSubmit : OwnerSignUpAction
    data object OnTogglePasswordVisibility : OwnerSignUpAction
}

/**
 * The business owner's sign-up, the only one there is (backend feature 009). On success the
 * repository signs in, the session changes and the navigation gate moves on by itself.
 */
class OwnerSignUpViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val name = TextFieldState()
    val document = TextFieldState()
    val email = TextFieldState()
    val password = TextFieldState()
    val code = TextFieldState()

    private val _state = MutableStateFlow(OwnerSignUpState())
    val state: StateFlow<OwnerSignUpState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            snapshotFlow { password.text.toString() }.collect { typed ->
                _state.update { it.copy(unmetRequirements = PasswordValidator.validate(typed), serverUnmetRequirements = emptySet()) }
            }
        }
    }

    fun onAction(action: OwnerSignUpAction) {
        when (action) {
            OwnerSignUpAction.OnSubmit -> submit()
            OwnerSignUpAction.OnTogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
        }
    }

    private fun submit() {
        if (_state.value.isLoading) return
        val registration = OwnerRegistration(
            name = name.text.toString().trim(),
            identityDocument = document.text.toString().trim(),
            email = email.text.toString().trim(),
            password = password.text.toString(),
            bootstrapCode = code.text.toString().trim()
        )
        val problems = buildMap {
            text(registration.name, OwnerRegistration.MAX_NAME)?.let { put(SignUpField.NAME, it) }
            text(registration.identityDocument, OwnerRegistration.MAX_DOCUMENT)?.let { put(SignUpField.DOCUMENT, it) }
            text(registration.bootstrapCode, OwnerRegistration.MAX_CODE)?.let { put(SignUpField.CODE, it) }
            if (registration.password.isEmpty()) put(SignUpField.PASSWORD, FieldProblem.REQUIRED)
        }
        val emailError = LoginViewModel.validateEmail(registration.email)
        val unmet = PasswordValidator.validate(registration.password)
        if (problems.isNotEmpty() || emailError != null || unmet.isNotEmpty()) {
            _state.update { it.copy(problems = problems, emailError = emailError, unmetRequirements = unmet, error = null) }
            return
        }
        _state.update { it.copy(isLoading = true, problems = emptyMap(), emailError = null, error = null) }
        viewModelScope.launch {
            val result = authRepository.registerOwner(registration)
            _state.update { state ->
                val error = (result as? Result.Failure)?.error
                when (error) {
                    null -> state.copy(isLoading = false)
                    AuthError.EmailTaken -> state.copy(isLoading = false, problems = mapOf(SignUpField.EMAIL to FieldProblem.TAKEN))
                    AuthError.AccountExists -> state.copy(isLoading = false, problems = mapOf(SignUpField.DOCUMENT to FieldProblem.TAKEN))
                    AuthError.InvalidDocument -> state.copy(isLoading = false, problems = mapOf(SignUpField.DOCUMENT to FieldProblem.INVALID))
                    is AuthError.WeakPassword -> state.copy(isLoading = false, serverUnmetRequirements = error.requirements)
                    else -> state.copy(isLoading = false, error = error)
                }
            }
        }
    }

    private fun text(value: String, max: Int): FieldProblem? = when {
        value.isEmpty() -> FieldProblem.REQUIRED
        value.length > max -> FieldProblem.TOO_LONG
        else -> null
    }
}
