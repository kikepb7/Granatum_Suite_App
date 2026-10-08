package com.granatum.feature.invoicing.presentation.company

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.invoicing.domain.model.FiscalLimits
import com.granatum.feature.invoicing.presentation.common.ErrorState
import com.granatum.feature.invoicing.presentation.common.LoadingState
import granatumsuite.feature.invoicing.presentation.generated.resources.*
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CompanyRoot(
    onNavigateBack: () -> Unit,
    viewModel: CompanyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.asString()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.messageShown()
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { AppTopBar(title = stringResource(Res.string.company_title), onBackClick = onNavigateBack) },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.loadError != null -> ErrorState(state.loadError!!, onRetry = viewModel::retry, Modifier.padding(padding))
            else ->
                Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val nameError =
                        when {
                            CompanyIssue.NAME_REQUIRED in state.issues -> stringResource(Res.string.required)
                            CompanyIssue.NAME_TOO_LONG in state.issues -> stringResource(Res.string.too_long, FiscalLimits.COMPANY_NAME)
                            else -> null
                        }
                    AppTextField(
                        state = viewModel.legalName,
                        title = stringResource(Res.string.company_legal_name),
                        singleLine = true,
                        isError =
                            nameError != null,
                        supportingText = nameError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val idError =
                        when {
                            CompanyIssue.TAX_ID_REQUIRED in state.issues -> stringResource(Res.string.required)
                            CompanyIssue.TAX_ID_TOO_LONG in state.issues -> stringResource(Res.string.too_long, FiscalLimits.TAX_ID)
                            CompanyIssue.TAX_ID_INVALID in state.issues -> stringResource(Res.string.error_invalid_tax_id)
                            else -> null
                        }
                    AppTextField(
                        state = viewModel.taxId,
                        title = stringResource(Res.string.company_tax_id),
                        singleLine = true,
                        isError =
                            idError != null,
                        supportingText = idError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.recognitionEnabled?.let { enabled ->
                        Text(
                            stringResource(if (enabled) Res.string.company_recognition_on else Res.string.company_recognition_off),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.extended.textSecondary,
                        )
                    }
                    AppButton(
                        stringResource(Res.string.save),
                        onClick = viewModel::save,
                        isLoading = state.isSaving,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
        }
    }
}
