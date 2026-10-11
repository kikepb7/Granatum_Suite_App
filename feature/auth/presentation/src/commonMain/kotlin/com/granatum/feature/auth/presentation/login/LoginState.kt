package com.granatum.feature.auth.presentation.login

import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.DemoAccount
import com.granatum.core.domain.environment.AppEnvironment
import com.granatum.core.domain.environment.EnvironmentError

enum class EmailFieldError { REQUIRED, INVALID, TOO_LONG }

enum class PasswordFieldError { REQUIRED, TOO_LONG }

data class LoginState(
    /** PRE build only (specs/010-entornos): the backend in use and the others it can switch to. */
    val environment: AppEnvironment = AppEnvironment.PRO,
    val environments: List<AppEnvironment> = emptyList(),
    val isSwitchingEnvironment: Boolean = false,
    val environmentError: EnvironmentError? = null,
    /** The ready-made accounts of the Demo environment; empty anywhere else. */
    val demoAccounts: List<DemoAccount> = emptyList(),
    val emailError: EmailFieldError? = null,
    val passwordError: PasswordFieldError? = null,
    val error: AuthError? = null,
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false
)

sealed interface LoginAction {
    data object OnSubmit : LoginAction
    data object OnTogglePasswordVisibility : LoginAction
    data class OnEnvironment(val environment: AppEnvironment) : LoginAction
    data class OnDemoAccount(val account: DemoAccount) : LoginAction
}
