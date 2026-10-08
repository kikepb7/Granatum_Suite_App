package com.granatum.feature.auth.presentation.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.core.domain.auth.model.DemoAccount
import com.granatum.core.domain.auth.model.DemoAccounts
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sign-in. Navigation does not happen here: a stored session changes the session state, and the
 * navigation gate reacts to that. This screen only has to get the credentials to the server.
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
    demoAccounts: DemoAccounts = DemoAccounts { emptyList() }
) : ViewModel() {

    /** Ready-made accounts of the demo build; empty in every other build. */
    val demoAccounts: List<DemoAccount> = demoAccounts.accounts()

    val emailState = TextFieldState()
    val passwordState = TextFieldState()

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    init {
        // Editing either field clears the errors it may have caused.
        merge(
            snapshotFlow { emailState.text.toString() }.drop(1),
            snapshotFlow { passwordState.text.toString() }.drop(1)
        ).onEach {
            _state.update { it.copy(emailError = null, passwordError = null, error = null) }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.OnSubmit -> submit()
            LoginAction.OnTogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is LoginAction.OnDemoAccount -> {
                emailState.setTextAndPlaceCursorAtEnd(action.account.email)
                passwordState.setTextAndPlaceCursorAtEnd(action.account.password)
                submit()
            }
        }
    }

    private fun submit() {
        // A second tap while the first request is in flight does nothing (FR-005).
        if (_state.value.isLoading) return

        val email = emailState.text.toString().trim()
        val password = passwordState.text.toString()
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError, error = null) }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.login(email = email, password = password)
            _state.update {
                it.copy(isLoading = false, error = (result as? Result.Failure)?.error)
            }
        }
    }

    companion object {
        /** Request field limits in docs/openapi.json (LoginRequest). */
        const val EMAIL_MAX_LENGTH = 254
        const val PASSWORD_MAX_LENGTH = 128

        private val EMAIL_SHAPE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        fun validateEmail(email: String): EmailFieldError? = when {
            email.isEmpty() -> EmailFieldError.REQUIRED
            email.length > EMAIL_MAX_LENGTH -> EmailFieldError.TOO_LONG
            !EMAIL_SHAPE.matches(email) -> EmailFieldError.INVALID
            else -> null
        }

        fun validatePassword(password: String): PasswordFieldError? = when {
            password.isEmpty() -> PasswordFieldError.REQUIRED
            password.length > PASSWORD_MAX_LENGTH -> PasswordFieldError.TOO_LONG
            else -> null
        }
    }
}
