package com.granatum.feature.sheets.presentation.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.cards.AppInsetCard
import com.granatum.core.designsystem.components.cards.AppStatCard
import com.granatum.core.designsystem.components.chips.AppStatusChip
import com.granatum.core.designsystem.components.chips.AppTone
import com.granatum.core.designsystem.components.dropdown.AppDropDownItemModel
import com.granatum.core.designsystem.components.dropdown.AppDropDownMenu
import com.granatum.core.designsystem.components.feedback.AppBanner
import com.granatum.core.designsystem.components.feedback.AppEmptyState
import com.granatum.core.designsystem.components.feedback.AppErrorState
import com.granatum.core.designsystem.components.feedback.AppLoadingState
import com.granatum.core.designsystem.components.lists.AppSectionHeader
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.components.topbar.TopBarIconButton
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import com.granatum.core.presentation.util.ObserveAsEvents
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.sheets.domain.model.SheetMonth
import granatumsuite.feature.sheets.presentation.generated.resources.Res
import granatumsuite.feature.sheets.presentation.generated.resources.config_account_label
import granatumsuite.feature.sheets.presentation.generated.resources.config_cancel
import granatumsuite.feature.sheets.presentation.generated.resources.config_change
import granatumsuite.feature.sheets.presentation.generated.resources.config_configured
import granatumsuite.feature.sheets.presentation.generated.resources.config_copy_email
import granatumsuite.feature.sheets.presentation.generated.resources.config_email_copied
import granatumsuite.feature.sheets.presentation.generated.resources.config_instructions
import granatumsuite.feature.sheets.presentation.generated.resources.config_instructions_detail
import granatumsuite.feature.sheets.presentation.generated.resources.config_invoices_label
import granatumsuite.feature.sheets.presentation.generated.resources.config_link_placeholder
import granatumsuite.feature.sheets.presentation.generated.resources.config_open_invoices
import granatumsuite.feature.sheets.presentation.generated.resources.config_open_shifts
import granatumsuite.feature.sheets.presentation.generated.resources.config_save
import granatumsuite.feature.sheets.presentation.generated.resources.config_shifts_label
import granatumsuite.feature.sheets.presentation.generated.resources.config_title
import granatumsuite.feature.sheets.presentation.generated.resources.drive_subtitle
import granatumsuite.feature.sheets.presentation.generated.resources.drive_title
import granatumsuite.feature.sheets.presentation.generated.resources.drive_unavailable
import granatumsuite.feature.sheets.presentation.generated.resources.drive_unavailable_hint
import granatumsuite.feature.sheets.presentation.generated.resources.export_button
import granatumsuite.feature.sheets.presentation.generated.resources.export_choose_month
import granatumsuite.feature.sheets.presentation.generated.resources.export_failed
import granatumsuite.feature.sheets.presentation.generated.resources.export_hint
import granatumsuite.feature.sheets.presentation.generated.resources.export_month
import granatumsuite.feature.sheets.presentation.generated.resources.export_running
import granatumsuite.feature.sheets.presentation.generated.resources.export_title
import granatumsuite.feature.sheets.presentation.generated.resources.resync_button
import granatumsuite.feature.sheets.presentation.generated.resources.resync_failed
import granatumsuite.feature.sheets.presentation.generated.resources.resync_hint
import granatumsuite.feature.sheets.presentation.generated.resources.resync_running
import granatumsuite.feature.sheets.presentation.generated.resources.resync_title
import granatumsuite.feature.sheets.presentation.generated.resources.retry
import granatumsuite.feature.sheets.presentation.generated.resources.status_error
import granatumsuite.feature.sheets.presentation.generated.resources.status_failed
import granatumsuite.feature.sheets.presentation.generated.resources.status_failed_title
import granatumsuite.feature.sheets.presentation.generated.resources.status_last_success
import granatumsuite.feature.sheets.presentation.generated.resources.status_never
import granatumsuite.feature.sheets.presentation.generated.resources.status_no_failures
import granatumsuite.feature.sheets.presentation.generated.resources.status_pending
import granatumsuite.feature.sheets.presentation.generated.resources.status_refresh
import granatumsuite.feature.sheets.presentation.generated.resources.status_retry
import granatumsuite.feature.sheets.presentation.generated.resources.status_retry_all
import granatumsuite.feature.sheets.presentation.generated.resources.status_title
import granatumsuite.feature.sheets.presentation.generated.resources.task_attempts
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Suppress("DEPRECATION") // LocalClipboard needs a platform ClipEntry; plain text is all this needs (as in staff).
@Composable
internal fun GoogleDriveScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoogleDriveViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    ObserveAsEvents(viewModel.uiEvents) { event ->
        when (event) {
            is GoogleDriveUiEvent.CopyToClipboard -> {
                clipboard.setText(AnnotatedString(event.text))
                snackbar.showSnackbar(getString(Res.string.config_email_copied))
            }
            is GoogleDriveUiEvent.ShowMessage -> snackbar.showSnackbar(event.message.asStringAsync())
        }
    }
    GoogleDriveContent(
        uiState = uiState,
        shiftsLink = viewModel.shiftsLink,
        invoicesLink = viewModel.invoicesLink,
        onAction = viewModel::onAction,
        onBack = onNavigateBack,
        snackbar = snackbar,
    )
}

@Composable
internal fun GoogleDriveContent(
    uiState: GoogleDriveScreenUiState,
    shiftsLink: TextFieldState,
    invoicesLink: TextFieldState,
    onAction: (GoogleDriveUiAction) -> Unit,
    onBack: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.drive_title),
                subtitle = stringResource(Res.string.drive_subtitle),
                onBackClick = onBack,
            )
        },
    ) { padding ->
        when (uiState) {
            GoogleDriveScreenUiState.Loading -> AppLoadingState(Modifier.padding(padding))
            is GoogleDriveScreenUiState.Error ->
                AppErrorState(
                    message = uiState.message.asString(),
                    retryLabel = stringResource(Res.string.retry),
                    onRetry = { onAction(GoogleDriveUiAction.Retry) },
                    modifier = Modifier.padding(padding),
                )
            GoogleDriveScreenUiState.Unavailable ->
                AppEmptyState(
                    title = stringResource(Res.string.drive_unavailable),
                    description = stringResource(Res.string.drive_unavailable_hint),
                    modifier = Modifier.padding(padding),
                )
            is GoogleDriveScreenUiState.Loaded ->
                LoadedContent(uiState.data, shiftsLink, invoicesLink, onAction, Modifier.padding(padding))
        }
    }
}

@Composable
private fun LoadedContent(
    data: GoogleDriveUiState,
    shiftsLink: TextFieldState,
    invoicesLink: TextFieldState,
    onAction: (GoogleDriveUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontal = AppTheme.spacing.screenHorizontal
    LazyColumn(
        contentPadding = PaddingValues(start = horizontal, end = horizontal, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "config-header") { Header(Res.string.config_title) }
        item(key = "config") { ConfigSection(data, shiftsLink, invoicesLink, onAction) }
        item(key = "export-header") { Header(Res.string.export_title) }
        item(key = "export") {
            MonthTaskSection(
                hint = Res.string.export_hint,
                months = data.months,
                selected = data.exportMonth,
                progress = data.export,
                button = Res.string.export_button,
                running = Res.string.export_running,
                failed = Res.string.export_failed,
                onSelect = { onAction(GoogleDriveUiAction.SelectExportMonth(it)) },
                onRun = { onAction(GoogleDriveUiAction.Export) },
            )
        }
        item(key = "resync-header") { Header(Res.string.resync_title) }
        item(key = "resync") {
            MonthTaskSection(
                hint = Res.string.resync_hint,
                months = data.months,
                selected = data.resyncMonth,
                progress = data.resync,
                button = Res.string.resync_button,
                running = Res.string.resync_running,
                failed = Res.string.resync_failed,
                onSelect = { onAction(GoogleDriveUiAction.SelectResyncMonth(it)) },
                onRun = { onAction(GoogleDriveUiAction.Resync) },
            )
        }
        item(key = "status-header") {
            AppSectionHeader(
                title = stringResource(Res.string.status_title),
                modifier = Modifier.semantics { heading() },
                action = {
                    TopBarIconButton(onClick = { onAction(GoogleDriveUiAction.RefreshStatus) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.status_refresh))
                    }
                },
            )
        }
        data.statusError?.let { error ->
            item(key = "status-error") {
                AppBanner(title = stringResource(Res.string.status_error), caption = error.asString(), tone = AppTone.DANGER)
            }
        }
        data.status?.let { status ->
            item(key = "status") { StatusSummary(status) }
            item(key = "failed-header") {
                AppSectionHeader(
                    title = stringResource(Res.string.status_failed_title),
                    action =
                        if (status.failed.size > 1) {
                            {
                                AppButton(
                                    text = stringResource(Res.string.status_retry_all),
                                    onClick = { onAction(GoogleDriveUiAction.RetryAllFailed) },
                                    style = AppButtonStyle.SECONDARY,
                                    isLoading = status.isRetryingAll,
                                    enabled = !status.isRetryingAll,
                                )
                            }
                        } else {
                            null
                        },
                )
            }
            if (status.failed.isEmpty()) {
                item(key = "no-failures") {
                    AppBanner(title = stringResource(Res.string.status_no_failures), tone = AppTone.SUCCESS)
                }
            }
            items(status.failed, key = { it.id }) { task ->
                FailedTaskCard(task, onRetry = { onAction(GoogleDriveUiAction.RetryTask(task.id)) })
            }
        }
    }
}

@Composable
private fun Header(title: StringResource) {
    AppSectionHeader(title = stringResource(title), modifier = Modifier.padding(top = 8.dp).semantics { heading() })
}

@Composable
private fun ConfigSection(
    data: GoogleDriveUiState,
    shiftsLink: TextFieldState,
    invoicesLink: TextFieldState,
    onAction: (GoogleDriveUiAction) -> Unit,
) {
    val config = data.config
    val uriHandler = LocalUriHandler.current
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(Res.string.config_account_label),
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.textTertiary,
            )
            AppInsetCard {
                Text(
                    text = data.serviceAccountEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                )
            }
            AppButton(
                text = stringResource(Res.string.config_copy_email),
                onClick = { onAction(GoogleDriveUiAction.CopyEmail) },
                style = AppButtonStyle.SECONDARY,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
            )
            AppBanner(
                title = stringResource(Res.string.config_instructions),
                caption = stringResource(Res.string.config_instructions_detail),
                tone = AppTone.INFO,
            )
            if (config.isEditing) {
                AppTextField(
                    state = shiftsLink,
                    title = stringResource(Res.string.config_shifts_label),
                    placeholder = stringResource(Res.string.config_link_placeholder),
                    supportingText = config.shiftsError?.asString(),
                    isError = config.shiftsError != null,
                    singleLine = true,
                    enabled = !config.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppTextField(
                    state = invoicesLink,
                    title = stringResource(Res.string.config_invoices_label),
                    placeholder = stringResource(Res.string.config_link_placeholder),
                    supportingText = config.invoicesError?.asString(),
                    isError = config.invoicesError != null,
                    singleLine = true,
                    enabled = !config.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                config.formError?.let { AppBanner(title = it.asString(), tone = AppTone.DANGER) }
                AppButton(
                    text = stringResource(Res.string.config_save),
                    onClick = { onAction(GoogleDriveUiAction.Save) },
                    isLoading = config.isSaving,
                    enabled = !config.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (config.isConfigured) {
                    AppButton(
                        text = stringResource(Res.string.config_cancel),
                        onClick = { onAction(GoogleDriveUiAction.CancelEditing) },
                        style = AppButtonStyle.SECONDARY,
                        enabled = !config.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                AppStatusChip(text = stringResource(Res.string.config_configured), tone = AppTone.SUCCESS, showDot = true)
                config.shiftsUrl?.let { url ->
                    OpenSheetButton(Res.string.config_open_shifts) { uriHandler.openUri(url) }
                }
                config.invoicesUrl?.let { url ->
                    OpenSheetButton(Res.string.config_open_invoices) { uriHandler.openUri(url) }
                }
                AppButton(
                    text = stringResource(Res.string.config_change),
                    onClick = { onAction(GoogleDriveUiAction.StartEditing) },
                    style = AppButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OpenSheetButton(
    label: StringResource,
    onClick: () -> Unit,
) {
    AppButton(
        text = stringResource(label),
        onClick = onClick,
        style = AppButtonStyle.SECONDARY,
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MonthTaskSection(
    hint: StringResource,
    months: List<MonthOptionUiState>,
    selected: MonthOptionUiState,
    progress: TaskProgressUiState,
    button: StringResource,
    running: StringResource,
    failed: StringResource,
    onSelect: (SheetMonth) -> Unit,
    onRun: () -> Unit,
) {
    val isRunning = progress == TaskProgressUiState.Running
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(hint), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
            MonthSelector(months, selected, enabled = !isRunning, onSelect = onSelect)
            AppButton(
                text = stringResource(button),
                onClick = onRun,
                isLoading = isRunning,
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth(),
            )
            when (progress) {
                TaskProgressUiState.Idle -> Unit
                TaskProgressUiState.Running -> {
                    LinearProgressIndicator(color = AppTheme.colors.brand, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(running), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                }
                is TaskProgressUiState.Done -> {
                    AppBanner(title = progress.message.asString(), tone = AppTone.SUCCESS)
                    progress.warning?.let { AppBanner(title = it.asString(), tone = AppTone.WARNING) }
                }
                is TaskProgressUiState.Queued -> AppBanner(title = progress.message.asString(), tone = AppTone.INFO)
                is TaskProgressUiState.Failed ->
                    AppBanner(title = stringResource(failed), caption = progress.message.asString(), tone = AppTone.DANGER)
            }
        }
    }
}

@Composable
private fun MonthSelector(
    months: List<MonthOptionUiState>,
    selected: MonthOptionUiState,
    enabled: Boolean,
    onSelect: (SheetMonth) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val chooseLabel = stringResource(Res.string.export_choose_month)
    val tint = AppTheme.colors.textPrimary
    val icon = Icons.Default.DateRange
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.export_month), style = MaterialTheme.typography.caption, color = AppTheme.colors.textTertiary)
        Box {
            AppButton(
                text = selected.label,
                onClick = { open = true },
                style = AppButtonStyle.SECONDARY,
                enabled = enabled,
                leadingIcon = { Icon(icon, contentDescription = chooseLabel) },
                modifier = Modifier.fillMaxWidth(),
            )
            AppDropDownMenu(
                isOpen = open,
                items =
                    months.map { option ->
                        AppDropDownItemModel(
                            title = option.label,
                            icon = icon,
                            contentColor = tint,
                            onClick = {
                                open = false
                                onSelect(option.month)
                            },
                        )
                    },
                onDismiss = { open = false },
            )
        }
    }
}

@Composable
private fun StatusSummary(status: SheetsStatusUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppStatCard(
            label = stringResource(Res.string.status_last_success),
            value = status.lastSuccessLabel ?: stringResource(Res.string.status_never),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AppStatCard(
                label = stringResource(Res.string.status_pending),
                value = status.pending.toString(),
                modifier = Modifier.weight(1f),
            )
            AppStatCard(
                label = stringResource(Res.string.status_failed),
                value = status.failedCount.toString(),
                valueColor = if (status.failedCount > 0) AppTheme.colors.danger else AppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FailedTaskCard(
    task: FailedTaskUiState,
    onRetry: () -> Unit,
) {
    AppCard(tone = AppTone.DANGER) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                )
                Text(text = task.reason.asString(), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.danger)
                Text(
                    text = stringResource(Res.string.task_attempts, task.attempts),
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.textTertiary,
                )
            }
            AppButton(
                text = stringResource(Res.string.status_retry),
                onClick = onRetry,
                style = AppButtonStyle.SECONDARY,
                isLoading = task.isRetrying,
                enabled = !task.isRetrying,
            )
        }
    }
}

private val previewMonths =
    SheetMonth.exportable(SheetMonth(2026, 10), count = 3).map { MonthOptionUiState(it, "${it.month}/${it.year}") }

private fun previewState(
    editing: Boolean,
    export: TaskProgressUiState,
) = GoogleDriveUiState(
    serviceAccountEmail = "granatum-demo@demo.iam.gserviceaccount.com",
    config =
        SheetsConfigUiState(
            shiftsUrl = if (editing) null else "https://docs.google.com/spreadsheets/d/1AbC/edit",
            invoicesUrl = if (editing) null else "https://docs.google.com/spreadsheets/d/1XyZ/edit",
            isEditing = editing,
            shiftsError = null,
            invoicesError = if (editing) UiText.DynamicString("La hoja no está compartida con la cuenta técnica.") else null,
            formError = null,
            isSaving = false,
        ),
    months = previewMonths,
    exportMonth = previewMonths.first(),
    export = export,
    resyncMonth = previewMonths.first(),
    resync = TaskProgressUiState.Idle,
    status =
        SheetsStatusUiState(
            lastSuccessLabel = "18/10/2026 12:05",
            pending = 0,
            failedCount = 1,
            failed =
                listOf(
                    FailedTaskUiState(
                        "t1",
                        UiText.DynamicString("Recibo de una factura"),
                        UiText.DynamicString("Hoja de recibos sin permiso de edición"),
                        8,
                        false,
                    ),
                ),
            isRetryingAll = false,
        ),
    statusError = null,
)

@Preview
@Composable
private fun GoogleDriveConfiguredPreview() {
    AppTheme {
        GoogleDriveContent(
            uiState =
                GoogleDriveScreenUiState.Loaded(
                    previewState(
                        editing = false,
                        export =
                            TaskProgressUiState.Done(
                                UiText.DynamicString("Exportados 92 fichajes de octubre de 2026"),
                                UiText.DynamicString("1 persona no tiene apellidos"),
                            ),
                    ),
                ),
            shiftsLink = rememberTextFieldState(),
            invoicesLink = rememberTextFieldState(),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun GoogleDriveSetUpPreview() {
    AppTheme {
        GoogleDriveContent(
            uiState = GoogleDriveScreenUiState.Loaded(previewState(editing = true, export = TaskProgressUiState.Running)),
            shiftsLink = rememberTextFieldState(),
            invoicesLink = rememberTextFieldState("https://docs.google.com/spreadsheets/d/no-compartida"),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun GoogleDriveUnavailablePreview() {
    AppTheme {
        GoogleDriveContent(
            uiState = GoogleDriveScreenUiState.Unavailable,
            shiftsLink = rememberTextFieldState(),
            invoicesLink = rememberTextFieldState(),
            onAction = {},
            onBack = {},
        )
    }
}
