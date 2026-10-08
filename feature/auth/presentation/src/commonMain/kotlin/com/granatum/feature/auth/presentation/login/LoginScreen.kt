package com.granatum.feature.auth.presentation.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.brand.AppBrandLogo
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.layouts.AppAdaptiveFormLayout
import com.granatum.core.designsystem.components.textfields.AppPasswordTextField
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.domain.auth.model.SignOutReason
import com.granatum.feature.auth.presentation.error.toUiText
import granatumsuite.feature.auth.presentation.generated.resources.Res
import granatumsuite.feature.auth.presentation.generated.resources.login_email
import granatumsuite.feature.auth.presentation.generated.resources.login_email_invalid
import granatumsuite.feature.auth.presentation.generated.resources.login_email_placeholder
import granatumsuite.feature.auth.presentation.generated.resources.login_email_required
import granatumsuite.feature.auth.presentation.generated.resources.login_email_too_long
import granatumsuite.feature.auth.presentation.generated.resources.login_forgot_hint
import granatumsuite.feature.auth.presentation.generated.resources.login_header
import granatumsuite.feature.auth.presentation.generated.resources.login_password
import granatumsuite.feature.auth.presentation.generated.resources.login_password_required
import granatumsuite.feature.auth.presentation.generated.resources.login_password_too_long
import granatumsuite.feature.auth.presentation.generated.resources.login_submit
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreenRoot(
    signOutReason: SignOutReason?,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        viewModel = viewModel,
        signOutReason = signOutReason
    )
}

@Composable
private fun LoginScreen(
    state: LoginState,
    viewModel: LoginViewModel,
    signOutReason: SignOutReason?
) {
    // A server error wins over the reason the last session ended: it is about what just happened.
    val bannerText = state.error?.toUiText()?.asString() ?: signOutReason?.toUiText()?.asString()

    AppAdaptiveFormLayout(
        headerText = stringResource(Res.string.login_header),
        errorText = bannerText,
        logo = { AppBrandLogo() }
    ) {
        AppTextField(
            state = viewModel.emailState,
            title = stringResource(Res.string.login_email),
            placeholder = stringResource(Res.string.login_email_placeholder),
            supportingText = state.emailError?.let { emailErrorText(it) },
            isError = state.emailError != null,
            singleLine = true,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        AppPasswordTextField(
            state = viewModel.passwordState,
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibilityClick = { viewModel.onAction(LoginAction.OnTogglePasswordVisibility) },
            title = stringResource(Res.string.login_password),
            supportingText = state.passwordError?.let { passwordErrorText(it) },
            isError = state.passwordError != null,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(
            text = stringResource(Res.string.login_submit),
            onClick = { viewModel.onAction(LoginAction.OnSubmit) },
            isLoading = state.isLoading,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.login_forgot_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.extended.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun emailErrorText(error: EmailFieldError): String = when (error) {
    EmailFieldError.REQUIRED -> stringResource(Res.string.login_email_required)
    EmailFieldError.INVALID -> stringResource(Res.string.login_email_invalid)
    EmailFieldError.TOO_LONG -> stringResource(Res.string.login_email_too_long, LoginViewModel.EMAIL_MAX_LENGTH)
}

@Composable
private fun passwordErrorText(error: PasswordFieldError): String = when (error) {
    PasswordFieldError.REQUIRED -> stringResource(Res.string.login_password_required)
    PasswordFieldError.TOO_LONG -> stringResource(Res.string.login_password_too_long, LoginViewModel.PASSWORD_MAX_LENGTH)
}
