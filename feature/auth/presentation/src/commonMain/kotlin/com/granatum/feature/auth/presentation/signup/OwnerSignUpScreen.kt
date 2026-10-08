package com.granatum.feature.auth.presentation.signup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.granatum.core.domain.auth.model.OwnerRegistration
import com.granatum.feature.auth.presentation.error.toUiText
import com.granatum.feature.auth.presentation.login.EmailFieldError
import com.granatum.feature.auth.presentation.login.LoginViewModel
import com.granatum.feature.auth.presentation.password.RequirementList
import granatumsuite.feature.auth.presentation.generated.resources.*
import granatumsuite.feature.auth.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OwnerSignUpRoot(onBackToLogin: () -> Unit, viewModel: OwnerSignUpViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val enabled = !state.isLoading

    AppAdaptiveFormLayout(
        headerText = stringResource(Res.string.signup_header),
        errorText = state.error?.toUiText()?.asString(),
        logo = { AppBrandLogo() }
    ) {
        Text(
            stringResource(Res.string.signup_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.extended.textSecondary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Field(viewModel.name, Res.string.signup_name, problem(state, SignUpField.NAME, OwnerRegistration.MAX_NAME), enabled)
        Field(viewModel.document, Res.string.signup_document, problem(state, SignUpField.DOCUMENT, OwnerRegistration.MAX_DOCUMENT), enabled)
        Field(
            viewModel.email, Res.string.login_email,
            problem(state, SignUpField.EMAIL, OwnerRegistration.MAX_EMAIL) ?: state.emailError?.let { emailText(it) },
            enabled, KeyboardType.Email
        )
        AppPasswordTextField(
            state = viewModel.password,
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibilityClick = { viewModel.onAction(OwnerSignUpAction.OnTogglePasswordVisibility) },
            title = stringResource(Res.string.login_password),
            isError = SignUpField.PASSWORD in state.problems || state.serverUnmetRequirements.isNotEmpty(),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        RequirementList(unmet = state.unmetRequirements + state.serverUnmetRequirements)
        Spacer(Modifier.height(16.dp))
        Field(viewModel.code, Res.string.signup_code, problem(state, SignUpField.CODE, OwnerRegistration.MAX_CODE), enabled)
        Text(
            stringResource(Res.string.signup_code_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.extended.textSecondary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        AppButton(
            text = stringResource(Res.string.signup_submit),
            onClick = { viewModel.onAction(OwnerSignUpAction.OnSubmit) },
            isLoading = state.isLoading,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBackToLogin, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.signup_back_to_login), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Field(
    state: androidx.compose.foundation.text.input.TextFieldState,
    title: org.jetbrains.compose.resources.StringResource,
    error: String?,
    enabled: Boolean,
    keyboard: KeyboardType = KeyboardType.Text
) {
    AppTextField(
        state = state,
        title = stringResource(title),
        supportingText = error,
        isError = error != null,
        singleLine = true,
        enabled = enabled,
        keyboardType = keyboard,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun problem(state: OwnerSignUpState, field: SignUpField, max: Int): String? = when (state.problems[field]) {
    FieldProblem.REQUIRED -> stringResource(Res.string.signup_required)
    FieldProblem.TOO_LONG -> stringResource(Res.string.signup_too_long, max)
    FieldProblem.INVALID -> stringResource(Res.string.signup_document_invalid)
    FieldProblem.TAKEN -> stringResource(if (field == SignUpField.EMAIL) Res.string.signup_email_taken else Res.string.signup_document_taken)
    null -> null
}

@Composable
private fun emailText(error: EmailFieldError): String = when (error) {
    EmailFieldError.REQUIRED -> stringResource(Res.string.login_email_required)
    EmailFieldError.INVALID -> stringResource(Res.string.login_email_invalid)
    EmailFieldError.TOO_LONG -> stringResource(Res.string.login_email_too_long, LoginViewModel.EMAIL_MAX_LENGTH)
}
