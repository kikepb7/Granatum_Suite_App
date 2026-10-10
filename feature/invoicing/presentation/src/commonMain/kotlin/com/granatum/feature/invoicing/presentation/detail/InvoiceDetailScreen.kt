package com.granatum.feature.invoicing.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppButton
import com.granatum.core.designsystem.components.buttons.AppButtonStyle
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.cards.AppInsetCard
import com.granatum.core.designsystem.components.dialogs.AppDestructiveConfirmationDialog
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.extended
import com.granatum.feature.invoicing.domain.model.FiscalLimits
import com.granatum.feature.invoicing.domain.model.Invoice
import com.granatum.feature.invoicing.domain.model.InvoiceField
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.InvoiceWarning
import com.granatum.feature.invoicing.domain.model.Money
import com.granatum.feature.invoicing.domain.model.NoQuotaCause
import com.granatum.feature.invoicing.presentation.common.ErrorState
import com.granatum.feature.invoicing.presentation.common.LoadingState
import com.granatum.feature.invoicing.presentation.common.SectionTitle
import com.granatum.feature.invoicing.presentation.common.StateBadge
import com.granatum.feature.invoicing.presentation.common.label
import com.granatum.feature.invoicing.presentation.list.toDate
import com.granatum.feature.invoicing.presentation.list.toPickerMillis
import granatumsuite.feature.invoicing.presentation.generated.resources.*
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InvoiceDetailRoot(
    invoiceId: String,
    onNavigateBack: () -> Unit,
    onOpenOriginal: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: InvoiceDetailViewModel = koinViewModel { parametersOf(invoiceId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.asString()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.onAction(InvoiceDetailAction.OnMessageShown)
        }
    }
    var leaving by remember { mutableStateOf(false) }
    val back = { if (state.invoice?.canEdit == true && viewModel.isDirty) leaving = true else onNavigateBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.extended.surfaceLower,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { AppTopBar(title = stringResource(Res.string.invoice_title), onBackClick = back) },
    ) { padding ->
        val invoice = state.invoice
        when {
            state.isLoading && invoice == null -> LoadingState(Modifier.padding(padding))
            invoice == null ->
                ErrorState(state.loadError ?: return@Scaffold, onRetry = {
                    viewModel.onAction(InvoiceDetailAction.OnRetry)
                }, Modifier.padding(padding))
            else -> DetailContent(invoice, state, viewModel, onOpenOriginal, onOpenHistory, Modifier.padding(padding))
        }
    }

    if (state.confirmingDiscard) {
        AppDestructiveConfirmationDialog(
            title = stringResource(Res.string.discard_title),
            description = stringResource(Res.string.discard_description),
            confirmButtonText = stringResource(Res.string.discard),
            cancelButtonText = stringResource(Res.string.cancel),
            onConfirmClick = { viewModel.onAction(InvoiceDetailAction.OnDiscardConfirm) },
            onCancelClick = { viewModel.onAction(InvoiceDetailAction.OnDiscardDismiss) },
            onDismiss = { viewModel.onAction(InvoiceDetailAction.OnDiscardDismiss) },
        )
    }
    if (state.confirmingRecognize) {
        AppDestructiveConfirmationDialog(
            title = stringResource(Res.string.recognize_title),
            description = stringResource(Res.string.recognize_description),
            confirmButtonText = stringResource(Res.string.recognize),
            cancelButtonText = stringResource(Res.string.cancel),
            onConfirmClick = { viewModel.onAction(InvoiceDetailAction.OnRecognizeConfirm) },
            onCancelClick = { viewModel.onAction(InvoiceDetailAction.OnRecognizeDismiss) },
            onDismiss = { viewModel.onAction(InvoiceDetailAction.OnRecognizeDismiss) },
        )
    }
    if (leaving) {
        AppDestructiveConfirmationDialog(
            title = stringResource(Res.string.unsaved_title),
            description = stringResource(Res.string.unsaved_description),
            confirmButtonText = stringResource(Res.string.leave),
            cancelButtonText = stringResource(Res.string.cancel),
            onConfirmClick = {
                leaving = false
                onNavigateBack()
            },
            onCancelClick = { leaving = false },
            onDismiss = { leaving = false },
        )
    }
}

/** Warnings, doubtful fields and local format errors, looked up by the server's field names. */
private class FieldMarks(
    invoice: Invoice,
    val badFields: Set<String>,
) {
    private val byField = invoice.warnings.groupBy { it.field }
    private val doubtful =
        invoice.recognition
            ?.doubtfulFields
            .orEmpty()
            .toSet()
    val shownFields = mutableSetOf<String>()

    fun warnings(field: String): List<InvoiceWarning> = byField[field].orEmpty().also { shownFields += field }

    fun isDoubtful(field: String) = field in doubtful && byField[field].orEmpty().none { it.blocking }

    /** Warnings on fields no editor showed (a single line, `general`…), listed at the end. */
    fun unplaced(): List<InvoiceWarning> = byField.filterKeys { it !in shownFields }.values.flatten()
}

@Composable
private fun DetailContent(
    invoice: Invoice,
    state: InvoiceDetailState,
    viewModel: InvoiceDetailViewModel,
    onOpenOriginal: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier,
) {
    val enabled = state.canEdit
    val marks = FieldMarks(invoice, state.badFields)
    val form = viewModel.form

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StateBadge(invoice.state)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenOriginal) { Text(stringResource(Res.string.original)) }
            TextButton(onClick = onOpenHistory) { Text(stringResource(Res.string.history)) }
        }
        when {
            invoice.state == InvoiceState.DISCARDED -> Notice(stringResource(Res.string.discarded_notice))
            invoice.state == InvoiceState.CONFIRMED && invoice.quarterClosed -> Notice(stringResource(Res.string.quarter_closed_notice))
            invoice.state == InvoiceState.PENDING_RECOGNITION -> Notice(stringResource(Res.string.pending_notice))
        }

        Card {
            SectionTitle(stringResource(Res.string.section_parties))
            Text(stringResource(Res.string.field_type), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InvoiceType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        enabled = enabled,
                        onClick = { viewModel.onAction(InvoiceDetailAction.OnType(if (state.type == type) null else type)) },
                        label = { Text(stringResource(type.label())) },
                    )
                }
            }
            Marks(marks.warnings(InvoiceField.TYPE), doubtful = marks.isDoubtful(InvoiceField.TYPE))
            Field(form.issuerName, Res.string.field_issuer_name, InvoiceField.ISSUER_NAME, marks, enabled, max = FiscalLimits.NAME)
            Field(form.issuerTaxId, Res.string.field_issuer_tax_id, InvoiceField.ISSUER_TAX_ID, marks, enabled, max = FiscalLimits.TAX_ID)
            Field(form.recipientName, Res.string.field_recipient_name, InvoiceField.RECIPIENT_NAME, marks, enabled, max = FiscalLimits.NAME)
            Field(
                form.recipientTaxId,
                Res.string.field_recipient_tax_id,
                InvoiceField.RECIPIENT_TAX_ID,
                marks,
                enabled,
                max = FiscalLimits.TAX_ID,
            )
        }

        Card {
            SectionTitle(stringResource(Res.string.section_invoice))
            Field(form.number, Res.string.field_number, InvoiceField.NUMBER, marks, enabled, max = FiscalLimits.NUMBER)
            DateField(state, marks, enabled) { viewModel.onAction(InvoiceDetailAction.OnIssueDate(it)) }
            Field(form.concept, Res.string.field_concept, InvoiceField.CONCEPT, marks, enabled, max = FiscalLimits.CONCEPT)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.field_corrective), modifier = Modifier.weight(1f))
                Switch(
                    checked = state.corrective,
                    enabled = enabled,
                    onCheckedChange = { viewModel.onAction(InvoiceDetailAction.OnCorrective(it)) },
                )
            }
        }

        Card {
            SectionTitle(stringResource(Res.string.section_lines))
            Marks(marks.warnings(InvoiceField.LINES), doubtful = marks.isDoubtful(InvoiceField.LINES))
            state.lineKeys.forEachIndexed { index, key ->
                LineEditor(
                    index,
                    viewModel.line(key),
                    state.causes[key],
                    marks,
                    enabled,
                    onCause = { viewModel.onAction(InvoiceDetailAction.OnCause(key, it)) },
                    onRemove = { viewModel.onAction(InvoiceDetailAction.OnRemoveLine(key)) },
                )
            }
            if (enabled) {
                AppButton(stringResource(Res.string.add_line), onClick = {
                    viewModel.onAction(InvoiceDetailAction.OnAddLine)
                }, style = AppButtonStyle.SECONDARY)
            }
        }

        Card {
            SectionTitle(stringResource(Res.string.section_totals))
            Field(
                form.withholding,
                Res.string.field_withholding,
                InvoiceField.WITHHOLDING,
                marks,
                enabled,
                keyboard = KeyboardType.Decimal,
                amount = true,
            )
            Field(form.total, Res.string.field_total, InvoiceField.TOTAL, marks, enabled, keyboard = KeyboardType.Decimal, amount = true)
            Balance(viewModel, enabled)
        }

        val unplaced = marks.unplaced()
        if (unplaced.isNotEmpty()) Card { Marks(unplaced, doubtful = false, showField = true) }

        Actions(invoice, state, viewModel)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun Notice(text: String) {
    AppInsetCard(contentPadding = 12.dp) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.extended.textSecondary,
        )
    }
}

@Composable
private fun Field(
    state: TextFieldState,
    title: org.jetbrains.compose.resources.StringResource,
    field: String,
    marks: FieldMarks,
    enabled: Boolean,
    max: Int? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    amount: Boolean = false,
) {
    val warnings = marks.warnings(field)
    val bad = field in marks.badFields
    val error =
        when {
            bad && amount -> stringResource(Res.string.invalid_amount)
            bad && max != null -> stringResource(Res.string.too_long, max)
            else -> null
        }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppTextField(
            state = state,
            title = stringResource(title),
            singleLine = true,
            enabled = enabled,
            keyboardType = keyboard,
            isError = bad || warnings.any { it.blocking },
            supportingText = error,
            modifier = Modifier.fillMaxWidth(),
        )
        Marks(warnings, doubtful = marks.isDoubtful(field))
    }
}

@Composable
private fun Marks(
    warnings: List<InvoiceWarning>,
    doubtful: Boolean,
    showField: Boolean = false,
) {
    if (doubtful) MarkText(stringResource(Res.string.doubtful), MaterialTheme.colorScheme.extended.yellowCardText)
    warnings.forEach { warning ->
        val prefix = if (warning.blocking) stringResource(Res.string.warning_blocking) + ": " else ""
        val text = if (showField) "$prefix${warning.message} (${warning.field})" else "$prefix${warning.message}"
        MarkText(text, if (warning.blocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.extended.yellowCardText)
    }
}

@Composable
private fun MarkText(
    text: String,
    color: Color,
) = Text(text, style = MaterialTheme.typography.bodySmall, color = color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    state: InvoiceDetailState,
    marks: FieldMarks,
    enabled: Boolean,
    onDate: (kotlinx.datetime.LocalDate?) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.field_issue_date), style = MaterialTheme.typography.labelMedium)
        AppButton(
            text = state.issueDate?.label() ?: stringResource(Res.string.choose_date),
            onClick = { picking = true },
            enabled = enabled,
            style = AppButtonStyle.SECONDARY,
        )
        Marks(marks.warnings(InvoiceField.ISSUE_DATE), doubtful = marks.isDoubtful(InvoiceField.ISSUE_DATE))
    }
    if (picking) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.issueDate?.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    onDate(picker.selectedDateMillis?.toDate())
                }) { Text(stringResource(Res.string.upload_done)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(Res.string.cancel)) } },
        ) { DatePicker(state = picker) }
    }
}

@Composable
private fun LineEditor(
    index: Int,
    line: LineForm,
    cause: NoQuotaCause?,
    marks: FieldMarks,
    enabled: Boolean,
    onCause: (NoQuotaCause?) -> Unit,
    onRemove: () -> Unit,
) {
    AppInsetCard(contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.line_title, index + 1),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                if (enabled) {
                    IconButton(
                        onClick = onRemove,
                    ) { Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.remove_line)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LineField(
                    line.rate,
                    Res.string.field_rate,
                    FormField.line(index, FormField.RATE),
                    marks,
                    enabled,
                    rate = true,
                    Modifier.weight(1f),
                )
                LineField(
                    line.base,
                    Res.string.field_base,
                    FormField.line(index, FormField.BASE),
                    marks,
                    enabled,
                    rate = false,
                    Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LineField(
                    line.quota,
                    Res.string.field_quota,
                    FormField.line(index, FormField.QUOTA),
                    marks,
                    enabled,
                    rate = false,
                    Modifier.weight(1f),
                )
                LineField(
                    line.surcharge,
                    Res.string.field_surcharge,
                    FormField.line(index, FormField.SURCHARGE),
                    marks,
                    enabled,
                    rate = false,
                    Modifier.weight(1f),
                )
            }
            if (Money.parse(line.quota.text.toString())?.isZero == true) {
                Text(stringResource(Res.string.field_no_quota_cause), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NoQuotaCause.entries.forEach { option ->
                        FilterChip(
                            selected = cause == option,
                            enabled = enabled,
                            onClick = { onCause(if (cause == option) null else option) },
                            label = { Text(stringResource(option.label()), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineField(
    state: TextFieldState,
    title: org.jetbrains.compose.resources.StringResource,
    field: String,
    marks: FieldMarks,
    enabled: Boolean,
    rate: Boolean,
    modifier: Modifier,
) {
    val bad = field in marks.badFields
    AppTextField(
        state = state,
        title = stringResource(title),
        singleLine = true,
        enabled = enabled,
        keyboardType = KeyboardType.Decimal,
        isError = bad,
        supportingText = if (bad) stringResource(if (rate) Res.string.invalid_rate else Res.string.invalid_amount) else null,
        modifier = modifier,
    )
}

/** Live help to balance the invoice: what the lines minus withholding add up to (research D3). */
@Composable
private fun Balance(
    viewModel: InvoiceDetailViewModel,
    enabled: Boolean,
) {
    val draft = viewModel.currentDraft() ?: return
    val expected = draft.expectedTotal
    val gap = draft.balanceGap
    when {
        draft.isBalanced ->
            Text(
                stringResource(Res.string.balance_ok, expected.format()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.extended.success,
            )
        draft.lines.isNotEmpty() ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (gap != null) {
                    Text(
                        stringResource(Res.string.balance_gap, gap.format(), expected.format()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (enabled) {
                    TextButton(onClick = { viewModel.onAction(InvoiceDetailAction.OnUseTotal(expected)) }) {
                        Text(stringResource(Res.string.balance_use, expected.format()))
                    }
                }
            }
    }
}

@Composable
private fun Actions(
    invoice: Invoice,
    state: InvoiceDetailState,
    viewModel: InvoiceDetailViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (invoice.canConfirm) {
            AppButton(
                text = stringResource(Res.string.confirm),
                onClick = { viewModel.onAction(InvoiceDetailAction.OnConfirm) },
                enabled = !state.isBusy,
                isLoading = state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (invoice.canEdit) {
            AppButton(
                text = stringResource(Res.string.save),
                onClick = { viewModel.onAction(InvoiceDetailAction.OnSave) },
                enabled = !state.isBusy,
                style = if (invoice.canConfirm) AppButtonStyle.SECONDARY else AppButtonStyle.PRIMARY,
                isLoading = state.isBusy && !invoice.canConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (invoice.canRecognize && state.recognitionEnabled) {
            AppButton(
                text = stringResource(Res.string.recognize),
                onClick = { viewModel.onAction(InvoiceDetailAction.OnRecognize) },
                enabled = !state.isBusy,
                style = AppButtonStyle.TEXT,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (invoice.canDiscard) {
            AppButton(
                text = stringResource(Res.string.discard),
                onClick = { viewModel.onAction(InvoiceDetailAction.OnDiscard) },
                enabled = !state.isBusy,
                style = AppButtonStyle.DESTRUCTIVE_SECONDARY,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
