package com.granatum.feature.auth.presentation.login

import com.granatum.core.domain.auth.AuthError

enum class EmailFieldError { REQUIRED, INVALID, TOO_LONG }

enum class PasswordFieldError { REQUIRED, TOO_LONG }

data class LoginState(
    val emailError: EmailFieldError? = null,
    val passwordError: PasswordFieldError? = null,
    val error: AuthError? = null,
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false
)

sealed interface LoginAction {
    data object OnSubmit : LoginAction
    data object OnTogglePasswordVisibility : LoginAction

    /** Demo build only: sign in straight away with a ready-made account. */
    data class OnDemoAccount(val account: com.granatum.core.domain.auth.model.DemoAccount) : LoginAction
}
