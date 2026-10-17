package com.granatum.feature.events.presentation.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.granatum.core.designsystem.components.buttons.AppPrimaryButton
import com.granatum.core.designsystem.components.cards.AppCard
import com.granatum.core.designsystem.components.feedback.AppLoadingState
import com.granatum.core.designsystem.components.icons.AppIcons
import com.granatum.core.designsystem.components.textfields.AppTextField
import com.granatum.core.designsystem.components.topbar.AppTopBar
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.designsystem.theme.caption
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.accept
import granatumsuite.feature.events.presentation.generated.resources.cancel
import granatumsuite.feature.events.presentation.generated.resources.choose_date
import granatumsuite.feature.events.presentation.generated.resources.field_date
import granatumsuite.feature.events.presentation.generated.resources.field_description
import granatumsuite.feature.events.presentation.generated.resources.field_description_hint
import granatumsuite.feature.events.presentation.generated.resources.field_name
import granatumsuite.feature.events.presentation.generated.resources.form_edit
import granatumsuite.feature.events.presentation.generated.resources.form_new
import granatumsuite.feature.events.presentation.generated.resources.form_save
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

@Composable
internal fun EventFormScreen(
    eventId: String?,
    onSaved: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EventFormViewModel = koinViewModel(key = "event-form-${eventId ?: "new"}") { parametersOf(eventId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is EventFormUiEvent.Saved -> onSaved(event.eventId)
                is EventFormUiEvent.ShowError -> snackbar.showSnackbar(event.message.asStringAsync())
            }
        }
    }
    EventFormContent(
        uiState = uiState,
        name = viewModel.name,
        description = viewModel.description,
        snackbar = snackbar,
        onAction = viewModel::onAction,
        onBack = onNavigateBack
    )
}

@Composable
internal fun EventFormContent(
    uiState: EventFormScreenUiState,
    name: TextFieldState,
    description: TextFieldState,
    onAction: (EventFormUiAction) -> Unit,
    onBack: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() }
) {
    val loaded = uiState as? EventFormScreenUiState.Loaded
    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            AppTopBar(
                title = stringResource(if (loaded?.data?.isEdit == true) Res.string.form_edit else Res.string.form_new),
                onBackClick = onBack
            )
        }
    ) { padding ->
        if (loaded == null) {
            AppLoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val data = loaded.data
        // Purely visual: whether the calendar is open (android-patterns, local UI state).
        var choosingDate by rememberSaveable { mutableStateOf(false) }
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppTheme.spacing.screenHorizontal, vertical = 12.dp)
        ) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        state = name,
                        title = stringResource(Res.string.field_name),
                        singleLine = true,
                        isError = data.nameError != null,
                        supportingText = data.nameError?.asString(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppTextField(
                        state = description,
                        title = stringResource(Res.string.field_description),
                        placeholder = stringResource(Res.string.field_description_hint),
                        isError = data.descriptionError != null,
                        supportingText = data.descriptionError?.asString(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            DateField(
                label = data.dateLabel,
                error = data.dateError?.asString(),
                onClick = { choosingDate = true }
            )
            AppPrimaryButton(
                text = stringResource(Res.string.form_save),
                onClick = { onAction(EventFormUiAction.Save) },
                isLoading = data.isSaving,
                enabled = !data.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp)
            )
        }
        if (choosingDate) {
            EventDatePicker(
                initial = data.date,
                minDate = data.minDate,
                onConfirm = { date ->
                    choosingDate = false
                    onAction(EventFormUiAction.DateSelected(date))
                },
                onDismiss = { choosingDate = false }
            )
        }
    }
}

/** The chosen day, or an invitation to choose one; opens the calendar. */
@Composable
private fun DateField(label: String?, error: String?, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Surface(
        onClick = onClick,
        shape = AppTheme.shapes.card,
        color = colors.surface,
        border = BorderStroke(1.dp, if (error != null) colors.danger else colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(18.dp)) {
            Icon(AppIcons.Calendar, contentDescription = null, tint = colors.brand, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.field_date), style = MaterialTheme.typography.caption, color = colors.textTertiary)
                Text(
                    label ?: stringResource(Res.string.choose_date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (label == null) colors.textSecondary else colors.textPrimary
                )
            }
        }
    }
    if (error != null) {
        Text(error, style = MaterialTheme.typography.bodySmall, color = colors.danger, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Material's calendar, limited to today onwards: an event cannot be set on a day gone by. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDatePicker(initial: LocalDate?, minDate: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val minMillis = minDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= minMillis

            override fun isSelectableYear(year: Int): Boolean = year >= minDate.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    } ?: onDismiss()
                }
            ) { Text(stringResource(Res.string.accept), color = AppTheme.colors.brand) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel), color = AppTheme.colors.textSecondary) } }
    ) {
        DatePicker(state = state)
    }
}

@Preview
@Composable
private fun EventFormContentPreview() {
    AppTheme {
        EventFormContent(
            uiState = EventFormScreenUiState.Loaded(
                EventFormUiState(
                    isEdit = false,
                    date = LocalDate(2027, 6, 13),
                    dateLabel = "sáb, 13 jun 2027",
                    minDate = LocalDate(2027, 6, 1),
                    nameError = null,
                    descriptionError = null,
                    dateError = null,
                    isSaving = false
                )
            ),
            name = TextFieldState("Evento 1"),
            description = TextFieldState("Boda de Juan y María"),
            onAction = {},
            onBack = {}
        )
    }
}
