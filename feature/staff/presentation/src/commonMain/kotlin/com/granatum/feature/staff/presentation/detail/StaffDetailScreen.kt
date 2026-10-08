package com.granatum.feature.staff.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.dialogs.AppBottomSheet
import com.granatum.core.designsystem.components.dialogs.AppDestructiveConfirmationDialog
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
import org.koin.core.parameter.parametersOf

@Composable
fun StaffDetailRoot(
    memberId: String,
    onNavigateBack: () -> Unit,
    viewModel: StaffDetailViewModel = koinViewModel { parametersOf(memberId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.asString()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.onAction(StaffDetailAction.OnMessageShown)
        }
    }
    val member = state.member
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { AppTopBar(title = member?.name ?: stringResource(Res.string.detail_title), onBackClick = onNavigateBack) },
    ) { padding ->
        when {
            state.isLoading ->
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            member == null ->
                Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    state.loadError?.let { Text(it.asString()) }
                    AppButton(
                        stringResource(Res.string.retry),
                        onClick = { viewModel.onAction(StaffDetailAction.OnRetry) },
                        style = AppButtonStyle.SECONDARY,
                    )
                }
            else -> {
                val enabled = !state.isBusy
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(Res.string.field_document), style = MaterialTheme.typography.labelMedium)
                    Text(member.identityDocument, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(Res.string.document_not_editable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.extended.textSecondary,
                    )
                    StaffTextField(viewModel.name, Res.string.field_name, state.issues[StaffField.NAME], StaffLimits.NAME, enabled)
                    StaffTextField(
                        viewModel.position,
                        Res.string.field_position,
                        state.issues[StaffField.POSITION],
                        StaffLimits.POSITION,
                        enabled,
                    )
                    ContractChips(state.contract, enabled) { viewModel.onAction(StaffDetailAction.OnContract(it)) }
                    StartDateField(state.startDate, enabled) { viewModel.onAction(StaffDetailAction.OnStartDate(it)) }
                    AppButton(stringResource(Res.string.detail_save), onClick = {
                        viewModel.onAction(StaffDetailAction.OnSave)
                    }, enabled = enabled, isLoading = state.isBusy, modifier = Modifier.fillMaxWidth())
                    AppButton(
                        stringResource(Res.string.detail_reset),
                        onClick = { viewModel.onAction(StaffDetailAction.OnReset) },
                        enabled = enabled,
                        style = AppButtonStyle.SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.isSelf) {
                        Text(
                            stringResource(Res.string.detail_self),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.extended.textSecondary,
                        )
                    } else {
                        AppButton(
                            stringResource(if (member.active) Res.string.detail_deactivate else Res.string.detail_reactivate),
                            onClick = { viewModel.onAction(StaffDetailAction.OnToggleActive) },
                            enabled = enabled,
                            style = if (member.active) AppButtonStyle.DESTRUCTIVE_SECONDARY else AppButtonStyle.SECONDARY,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (state.confirmingDeactivate && member != null) {
        AppDestructiveConfirmationDialog(
            title = stringResource(Res.string.detail_deactivate_title, member.name),
            description = stringResource(Res.string.detail_deactivate_description),
            confirmButtonText = stringResource(Res.string.detail_deactivate),
            cancelButtonText = stringResource(Res.string.cancel),
            onConfirmClick = { viewModel.onAction(StaffDetailAction.OnDeactivateConfirm) },
            onCancelClick = { viewModel.onAction(StaffDetailAction.OnDismiss) },
            onDismiss = { viewModel.onAction(StaffDetailAction.OnDismiss) },
        )
    }
    if (state.confirmingReset && member != null) {
        AppDestructiveConfirmationDialog(
            title = stringResource(Res.string.detail_reset_title, member.name),
            description = stringResource(Res.string.detail_reset_description),
            confirmButtonText = stringResource(Res.string.detail_reset_confirm),
            cancelButtonText = stringResource(Res.string.cancel),
            onConfirmClick = { viewModel.onAction(StaffDetailAction.OnResetConfirm) },
            onCancelClick = { viewModel.onAction(StaffDetailAction.OnDismiss) },
            onDismiss = { viewModel.onAction(StaffDetailAction.OnDismiss) },
        )
    }
    if (state.grantingAccess && member != null) {
        AppBottomSheet(onDismiss = { viewModel.onAction(StaffDetailAction.OnDismiss) }) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.detail_no_account_title, member.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(Res.string.detail_no_account_description), style = MaterialTheme.typography.bodySmall)
                StaffTextField(
                    viewModel.grantEmail,
                    Res.string.field_email,
                    state.issues[StaffField.EMAIL],
                    StaffLimits.EMAIL,
                    keyboard = KeyboardType.Email,
                )
                RoleChips(state.grantRole, enabled = true) { viewModel.onAction(StaffDetailAction.OnGrantRole(it)) }
                AppButton(stringResource(Res.string.detail_grant), onClick = {
                    viewModel.onAction(StaffDetailAction.OnGrantConfirm)
                }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    state.credentials?.let { credentials ->
        CredentialsSheet(credentials, member?.name.orEmpty()) { viewModel.onAction(StaffDetailAction.OnCredentialsDismissed) }
    }
}
