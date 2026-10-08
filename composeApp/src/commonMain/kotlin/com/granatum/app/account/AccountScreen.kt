package com.granatum.app.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.avatar.AppAvatarPhoto
import com.granatum.core.designsystem.components.avatar.AvatarSize
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.dialogs.AppDestructiveConfirmationDialog
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.core.domain.auth.model.UserRole
import granatumsuite.composeapp.generated.resources.Res
import granatumsuite.composeapp.generated.resources.account_change_password
import granatumsuite.composeapp.generated.resources.account_role
import granatumsuite.composeapp.generated.resources.account_sign_out
import granatumsuite.composeapp.generated.resources.account_signed_in_as
import granatumsuite.composeapp.generated.resources.account_title
import granatumsuite.composeapp.generated.resources.role_admin
import granatumsuite.composeapp.generated.resources.role_employee
import granatumsuite.composeapp.generated.resources.role_manager
import granatumsuite.composeapp.generated.resources.role_representative
import granatumsuite.composeapp.generated.resources.role_unknown
import granatumsuite.composeapp.generated.resources.sign_out_pending_cancel
import granatumsuite.composeapp.generated.resources.sign_out_pending_confirm
import granatumsuite.composeapp.generated.resources.sign_out_pending_description
import granatumsuite.composeapp.generated.resources.sign_out_pending_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreenRoot(
    onChangePasswordClick: () -> Unit,
    viewModel: AccountViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(title = stringResource(Res.string.account_title)) }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            AppAvatarPhoto(displayText = state.initial, size = AvatarSize.LARGE)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.account_signed_in_as),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.textSecondary
            )
            Text(
                text = state.email,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.extended.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(Res.string.account_role, roleName(state.role)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.extended.textSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            AppButton(
                text = stringResource(Res.string.account_change_password),
                onClick = onChangePasswordClick,
                style = AppButtonStyle.SECONDARY,
                enabled = !state.isSigningOut,
                modifier = Modifier.fillMaxWidth()
            )
            AppButton(
                text = stringResource(Res.string.account_sign_out),
                onClick = { viewModel.onAction(AccountAction.OnSignOutClick) },
                style = AppButtonStyle.DESTRUCTIVE_SECONDARY,
                isLoading = state.isSigningOut,
                enabled = !state.isSigningOut,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (state.showPendingWarning) {
        AppDestructiveConfirmationDialog(
            title = stringResource(Res.string.sign_out_pending_title),
            description = pluralStringResource(Res.plurals.sign_out_pending_description, state.pendingCount, state.pendingCount),
            confirmButtonText = stringResource(Res.string.sign_out_pending_confirm),
            cancelButtonText = stringResource(Res.string.sign_out_pending_cancel),
            onConfirmClick = { viewModel.onAction(AccountAction.OnConfirmSignOut) },
            onCancelClick = { viewModel.onAction(AccountAction.OnDismissWarning) },
            onDismiss = { viewModel.onAction(AccountAction.OnDismissWarning) }
        )
    }
}

@Composable
fun roleName(role: UserRole): String = stringResource(
    when (role) {
        UserRole.ADMIN -> Res.string.role_admin
        UserRole.ENCARGADO -> Res.string.role_manager
        UserRole.EMPLEADO -> Res.string.role_employee
        UserRole.REPRESENTANTE -> Res.string.role_representative
        UserRole.DESCONOCIDO -> Res.string.role_unknown
    }
)
