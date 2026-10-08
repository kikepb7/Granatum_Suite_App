package com.granatum.feature.invoicing.presentation.quarters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.dialogs.AppBottomSheet
import com.granatum.core.designsystem.components.dialogs.AppDestructiveConfirmationDialog
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.invoicing.domain.model.QuarterAction
import com.granatum.feature.invoicing.presentation.common.ErrorState
import com.granatum.feature.invoicing.presentation.common.LoadingState
import com.granatum.feature.invoicing.presentation.common.Pill
import com.granatum.feature.invoicing.presentation.common.label
import granatumsuite.feature.invoicing.presentation.generated.resources.*
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QuartersRoot(
    onNavigateBack: () -> Unit,
    viewModel: QuartersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.asString()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.onAction(QuartersAction.OnMessageShown)
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { AppTopBar(title = stringResource(Res.string.quarters_title), onBackClick = onNavigateBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            ) {
                IconButton(onClick = { viewModel.onAction(QuartersAction.OnYear(-1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.previous_year))
                }
                Text(state.year.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.onAction(QuartersAction.OnYear(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(Res.string.next_year))
                }
            }
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, onRetry = { viewModel.onAction(QuartersAction.OnRetry) })
                else ->
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.quarters.forEach { quarter ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.extended.surfaceHigher,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            stringResource(Res.string.quarter_label, quarter.quarter, quarter.year),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (quarter.closed) {
                                            Pill(
                                                stringResource(Res.string.quarter_closed),
                                                MaterialTheme.colorScheme.extended.secondaryFill,
                                                MaterialTheme.colorScheme.extended.textSecondary,
                                            )
                                        } else {
                                            Pill(
                                                stringResource(Res.string.quarter_open),
                                                MaterialTheme.colorScheme.extended.successOutline,
                                                MaterialTheme.colorScheme.extended.success,
                                            )
                                        }
                                    }
                                    quarter.events.take(3).forEach { event ->
                                        val text =
                                            when (event.action) {
                                                QuarterAction.REOPEN ->
                                                    stringResource(
                                                        Res.string.quarter_event_reopen,
                                                        event.at.label(),
                                                        event.reason.orEmpty(),
                                                    )
                                                else -> stringResource(Res.string.quarter_event_close, event.at.label())
                                            }
                                        Text(
                                            text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.extended.textSecondary,
                                        )
                                    }
                                    AppButton(
                                        text = stringResource(if (quarter.closed) Res.string.quarter_reopen else Res.string.quarter_close),
                                        onClick = {
                                            viewModel.onAction(
                                                if (quarter.closed) QuartersAction.OnReopen(quarter) else QuartersAction.OnClose(quarter),
                                            )
                                        },
                                        enabled = !state.isBusy,
                                        style = AppButtonStyle.SECONDARY,
                                    )
                                }
                            }
                        }
                    }
            }
        }
    }

    state.closing?.let { quarter ->
        AppDestructiveConfirmationDialog(
            title = stringResource(Res.string.quarter_close_title, quarter.quarter, quarter.year),
            description = stringResource(Res.string.quarter_close_description),
            confirmButtonText = stringResource(Res.string.quarter_close),
            cancelButtonText = stringResource(Res.string.cancel),
            onConfirmClick = { viewModel.onAction(QuartersAction.OnCloseConfirm) },
            onCancelClick = { viewModel.onAction(QuartersAction.OnDismiss) },
            onDismiss = { viewModel.onAction(QuartersAction.OnDismiss) },
        )
    }
    state.reopening?.let { quarter ->
        AppBottomSheet(onDismiss = { viewModel.onAction(QuartersAction.OnDismiss) }) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.quarter_reopen_title, quarter.quarter, quarter.year),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                AppTextField(
                    state = viewModel.reason,
                    title = stringResource(Res.string.quarter_reopen_reason),
                    isError = state.reasonInvalid,
                    supportingText = if (state.reasonInvalid) stringResource(Res.string.quarter_reason_invalid) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppButton(stringResource(Res.string.quarter_reopen), onClick = {
                    viewModel.onAction(QuartersAction.OnReopenConfirm)
                }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
