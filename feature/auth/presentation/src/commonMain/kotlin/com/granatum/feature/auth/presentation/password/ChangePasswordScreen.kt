package com.granatum.feature.auth.presentation.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.brand.AppBrandLogo
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.layouts.AppAdaptiveFormLayout
import com.granatum.core.designsystem.components.textfields.AppPasswordTextField
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.domain.validation.PasswordRequirement
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.feature.auth.presentation.error.toUiText
import granatumsuite.feature.auth.presentation.generated.resources.Res
import granatumsuite.feature.auth.presentation.generated.resources.password_cancel
import granatumsuite.feature.auth.presentation.generated.resources.password_current
import granatumsuite.feature.auth.presentation.generated.resources.password_current_wrong
import granatumsuite.feature.auth.presentation.generated.resources.password_mandatory_header
import granatumsuite.feature.auth.presentation.generated.resources.password_mismatch
import granatumsuite.feature.auth.presentation.generated.resources.password_new
import granatumsuite.feature.auth.presentation.generated.resources.password_repeat
import granatumsuite.feature.auth.presentation.generated.resources.password_requirement_met
import granatumsuite.feature.auth.presentation.generated.resources.password_requirement_unmet
import granatumsuite.feature.auth.presentation.generated.resources.password_requirements_title
import granatumsuite.feature.auth.presentation.generated.resources.password_sign_out
import granatumsuite.feature.auth.presentation.generated.resources.password_submit
import granatumsuite.feature.auth.presentation.generated.resources.password_voluntary_header
import granatumsuite.feature.auth.presentation.generated.resources.login_password_required
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * @param onDismiss mandatory mode: sign out (the only other thing the server allows);
 * voluntary mode: go back.
 * @param onChanged voluntary mode only; a mandatory change moves the session state on by itself.
 */
@Composable
fun ChangePasswordScreenRoot(
    mode: ChangePasswordMode,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    viewModel: ChangePasswordViewModel = koinViewModel { parametersOf(mode) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(flow = viewModel.events) { event ->
        when (event) {
            ChangePasswordEvent.Changed -> onChanged()
            ChangePasswordEvent.Dismissed -> onDismiss()
        }
    }
    ChangePasswordScreen(state = state, viewModel = viewModel)
}

@Composable
private fun ChangePasswordScreen(
    state: ChangePasswordState,
    viewModel: ChangePasswordViewModel
) {
    val mandatory = state.mode == ChangePasswordMode.MANDATORY
    AppAdaptiveFormLayout(
        headerText = stringResource(
            if (mandatory) Res.string.password_mandatory_header else Res.string.password_voluntary_header
        ),
        errorText = state.error?.toUiText()?.asString(),
        logo = { AppBrandLogo() }
    ) {
        AppPasswordTextField(
            state = viewModel.currentState,
            isPasswordVisible = state.arePasswordsVisible,
            onToggleVisibilityClick = { viewModel.onAction(ChangePasswordAction.OnToggleVisibility) },
            title = stringResource(Res.string.password_current),
            supportingText = when {
                state.currentPasswordWrong -> stringResource(Res.string.password_current_wrong)
                state.currentPasswordMissing -> stringResource(Res.string.login_password_required)
                else -> null
            },
            isError = state.currentPasswordWrong || state.currentPasswordMissing,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        AppPasswordTextField(
            state = viewModel.newState,
            isPasswordVisible = state.arePasswordsVisible,
            onToggleVisibilityClick = { viewModel.onAction(ChangePasswordAction.OnToggleVisibility) },
            title = stringResource(Res.string.password_new),
            isError = state.serverUnmetRequirements.isNotEmpty(),
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        RequirementList(
            unmet = state.unmetRequirements + state.serverUnmetRequirements
        )
        Spacer(modifier = Modifier.height(16.dp))
        AppPasswordTextField(
            state = viewModel.repeatState,
            isPasswordVisible = state.arePasswordsVisible,
            onToggleVisibilityClick = { viewModel.onAction(ChangePasswordAction.OnToggleVisibility) },
            title = stringResource(Res.string.password_repeat),
            supportingText = if (state.repeatMismatch) stringResource(Res.string.password_mismatch) else null,
            isError = state.repeatMismatch,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        AppButton(
            text = stringResource(Res.string.password_submit),
            onClick = { viewModel.onAction(ChangePasswordAction.OnSubmit) },
            isLoading = state.isLoading,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        AppButton(
            text = stringResource(if (mandatory) Res.string.password_sign_out else Res.string.password_cancel),
            onClick = { viewModel.onAction(ChangePasswordAction.OnDismiss) },
            style = AppButtonStyle.TEXT,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Icon and text, never colour alone, so the state reads without colour vision (principle IX). */
@Composable
private fun RequirementList(unmet: Set<PasswordRequirement>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.password_requirements_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.extended.textSecondary
        )
        // The maximum is only worth showing once it is broken.
        PasswordRequirement.entries
            .filter { it != PasswordRequirement.LONGITUD_MAXIMA || it in unmet }
            .forEach { requirement ->
                val met = requirement !in unmet
                val label = requirement.toUiText().asString()
                val status = stringResource(
                    if (met) Res.string.password_requirement_met else Res.string.password_requirement_unmet
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $status" }
                ) {
                    Icon(
                        imageVector = if (met) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (met) MaterialTheme.colorScheme.extended.success else MaterialTheme.colorScheme.extended.textPlaceholder,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (met) MaterialTheme.colorScheme.extended.textSecondary else MaterialTheme.colorScheme.extended.textPrimary
                    )
                }
            }
    }
}
