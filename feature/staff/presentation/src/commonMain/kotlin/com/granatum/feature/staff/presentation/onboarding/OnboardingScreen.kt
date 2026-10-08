package com.granatum.feature.staff.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.staff.domain.StaffLimits
import com.granatum.feature.staff.presentation.common.ContractChips
import com.granatum.feature.staff.presentation.common.CredentialsSheet
import com.granatum.feature.staff.presentation.common.RoleChips
import com.granatum.feature.staff.presentation.common.StaffField
import com.granatum.feature.staff.presentation.common.StaffTextField
import com.granatum.feature.staff.presentation.common.StartDateField
import granatumsuite.feature.staff.presentation.generated.resources.*
import granatumsuite.feature.staff.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoot(
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val enabled = !state.isSaving
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.onboard_title), onBackClick = onNavigateBack) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.onboard_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.textSecondary,
            )
            state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            StaffTextField(viewModel.name, Res.string.field_name, state.issues[StaffField.NAME], StaffLimits.NAME, enabled)
            StaffTextField(
                viewModel.document,
                Res.string.field_document,
                state.issues[StaffField.DOCUMENT],
                StaffLimits.DOCUMENT,
                enabled,
                invalidText = Res.string.error_invalid_document,
            )
            StaffTextField(viewModel.position, Res.string.field_position, state.issues[StaffField.POSITION], StaffLimits.POSITION, enabled)
            ContractChips(state.contract, enabled, viewModel::setContract)
            StartDateField(state.startDate, enabled, viewModel::setStartDate)
            StaffTextField(
                viewModel.email,
                Res.string.field_email,
                state.issues[StaffField.EMAIL],
                StaffLimits.EMAIL,
                enabled,
                KeyboardType.Email,
            )
            RoleChips(state.role, enabled, viewModel::setRole)
            AppButton(
                stringResource(Res.string.onboard_submit),
                onClick = viewModel::submit,
                isLoading = state.isSaving,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    state.credentials?.let { credentials ->
        CredentialsSheet(credentials, state.onboardedName) {
            viewModel.credentialsDismissed()
            onDone()
        }
    }
}
