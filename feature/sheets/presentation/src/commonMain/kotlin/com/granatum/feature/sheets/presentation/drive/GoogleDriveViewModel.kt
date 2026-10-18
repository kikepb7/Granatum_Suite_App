package com.granatum.feature.sheets.presentation.drive

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.sheets.domain.model.SheetMonth
import com.granatum.feature.sheets.domain.model.SheetTask
import com.granatum.feature.sheets.domain.model.SheetTaskOutcome
import com.granatum.feature.sheets.domain.model.SheetTaskStatus
import com.granatum.feature.sheets.domain.model.SheetsError
import com.granatum.feature.sheets.domain.model.SheetsSettings
import com.granatum.feature.sheets.domain.model.SheetsStatus
import com.granatum.feature.sheets.domain.model.SpreadsheetField
import com.granatum.feature.sheets.domain.usecase.AwaitSheetTaskUseCase
import com.granatum.feature.sheets.domain.usecase.CurrentMonthProvider
import com.granatum.feature.sheets.domain.usecase.ExportShiftMonthUseCase
import com.granatum.feature.sheets.domain.usecase.GetSheetsSettingsUseCase
import com.granatum.feature.sheets.domain.usecase.GetSheetsStatusUseCase
import com.granatum.feature.sheets.domain.usecase.ResyncInvoiceMonthUseCase
import com.granatum.feature.sheets.domain.usecase.RetryAllFailedSheetTasksUseCase
import com.granatum.feature.sheets.domain.usecase.RetrySheetTaskUseCase
import com.granatum.feature.sheets.domain.usecase.SaveSheetsSettingsUseCase
import com.granatum.feature.sheets.presentation.common.SheetsDateFormatter
import com.granatum.feature.sheets.presentation.common.reasonText
import com.granatum.feature.sheets.presentation.common.titleText
import com.granatum.feature.sheets.presentation.common.toUiText
import granatumsuite.feature.sheets.presentation.generated.resources.Res
import granatumsuite.feature.sheets.presentation.generated.resources.config_link_required
import granatumsuite.feature.sheets.presentation.generated.resources.config_saved
import granatumsuite.feature.sheets.presentation.generated.resources.export_done
import granatumsuite.feature.sheets.presentation.generated.resources.export_done_one
import granatumsuite.feature.sheets.presentation.generated.resources.export_last_names_pending
import granatumsuite.feature.sheets.presentation.generated.resources.export_last_names_pending_one
import granatumsuite.feature.sheets.presentation.generated.resources.export_queued
import granatumsuite.feature.sheets.presentation.generated.resources.resync_done
import granatumsuite.feature.sheets.presentation.generated.resources.resync_done_one
import granatumsuite.feature.sheets.presentation.generated.resources.resync_queued
import granatumsuite.feature.sheets.presentation.generated.resources.status_retried
import granatumsuite.feature.sheets.presentation.generated.resources.status_retried_all
import granatumsuite.feature.sheets.presentation.generated.resources.status_retried_all_one
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/** The typed values behind the screen; [GoogleDriveScreenUiState] is derived from them. */
private data class GoogleDriveInternalState(
    val isLoading: Boolean = true,
    val loadError: SheetsError? = null,
    val settings: SheetsSettings? = null,
    val editing: Boolean = false,
    val shiftsError: UiText? = null,
    val invoicesError: UiText? = null,
    val formError: UiText? = null,
    val isSaving: Boolean = false,
    val exportMonth: SheetMonth,
    val export: TaskProgressUiState = TaskProgressUiState.Idle,
    val resyncMonth: SheetMonth,
    val resync: TaskProgressUiState = TaskProgressUiState.Idle,
    val status: SheetsStatus? = null,
    val statusError: UiText? = null,
    val retrying: Set<String> = emptySet(),
    val retryingAll: Boolean = false,
)

/**
 * The «Google Drive» screen (specs/013-google-sheets): the two spreadsheets, the export of a month
 * of shifts, the resync of a month of receipts and the state of the copy. Everything goes to the
 * server; nothing is kept on the device.
 */
internal class GoogleDriveViewModel(
    private val getSettings: GetSheetsSettingsUseCase,
    private val saveSettings: SaveSheetsSettingsUseCase,
    private val exportShiftMonth: ExportShiftMonthUseCase,
    private val resyncInvoiceMonth: ResyncInvoiceMonthUseCase,
    private val awaitTask: AwaitSheetTaskUseCase,
    private val getStatus: GetSheetsStatusUseCase,
    private val retryTask: RetrySheetTaskUseCase,
    private val retryAllFailed: RetryAllFailedSheetTasksUseCase,
    currentMonth: CurrentMonthProvider,
    private val dates: SheetsDateFormatter,
) : ViewModel() {
    /** The link of the shifts spreadsheet, or its id. */
    val shiftsLink = TextFieldState()

    /** The link of the receipts spreadsheet, or its id. */
    val invoicesLink = TextFieldState()

    /** From the current month backwards: no future months (US3, scenario 2). */
    private val months: List<SheetMonth> = SheetMonth.exportable(currentMonth.current())

    private val state = MutableStateFlow(GoogleDriveInternalState(exportMonth = months.first(), resyncMonth = months.first()))
    private val _uiEvents = MutableSharedFlow<GoogleDriveUiEvent>()
    val uiEvents: SharedFlow<GoogleDriveUiEvent> = _uiEvents.asSharedFlow()

    val uiState: StateFlow<GoogleDriveScreenUiState> =
        state
            .map { it.toUiState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, GoogleDriveScreenUiState.Loading)

    init {
        load()
    }

    fun onAction(action: GoogleDriveUiAction) {
        when (action) {
            GoogleDriveUiAction.Retry -> load()
            GoogleDriveUiAction.CopyEmail -> copyEmail()
            GoogleDriveUiAction.StartEditing -> startEditing()
            GoogleDriveUiAction.CancelEditing ->
                state.update {
                    it.copy(
                        editing = false,
                        shiftsError = null,
                        invoicesError = null,
                        formError = null,
                    )
                }
            GoogleDriveUiAction.Save -> save()
            is GoogleDriveUiAction.SelectExportMonth ->
                if (action.month in months) state.update { it.copy(exportMonth = action.month, export = TaskProgressUiState.Idle) }
            GoogleDriveUiAction.Export -> export()
            is GoogleDriveUiAction.SelectResyncMonth ->
                if (action.month in months) state.update { it.copy(resyncMonth = action.month, resync = TaskProgressUiState.Idle) }
            GoogleDriveUiAction.Resync -> resync()
            GoogleDriveUiAction.RefreshStatus -> viewModelScope.launch { loadStatus() }
            is GoogleDriveUiAction.RetryTask -> retry(action.taskId)
            GoogleDriveUiAction.RetryAllFailed -> retryAll()
        }
    }

    private fun load() {
        state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = getSettings.get()) {
                is Result.Failure -> state.update { it.copy(isLoading = false, loadError = result.error) }
                is Result.Success -> {
                    val settings = result.data
                    fillLinks(settings)
                    state.update { it.copy(isLoading = false, settings = settings, editing = !settings.isConfigured) }
                    if (settings.available) loadStatus()
                }
            }
        }
    }

    private suspend fun loadStatus() {
        when (val result = getStatus.get()) {
            is Result.Success -> state.update { it.copy(status = result.data, statusError = null) }
            is Result.Failure -> state.update { it.copy(statusError = result.error.toUiText()) }
        }
    }

    private fun fillLinks(settings: SheetsSettings) {
        shiftsLink.setTextAndPlaceCursorAtEnd(settings.shifts?.url.orEmpty())
        invoicesLink.setTextAndPlaceCursorAtEnd(settings.invoices?.url.orEmpty())
    }

    private fun copyEmail() {
        val email = state.value.settings?.serviceAccountEmail ?: return
        viewModelScope.launch { _uiEvents.emit(GoogleDriveUiEvent.CopyToClipboard(email)) }
    }

    private fun startEditing() {
        state.value.settings?.let(::fillLinks)
        state.update { it.copy(editing = true, shiftsError = null, invoicesError = null, formError = null) }
    }

    private fun save() {
        if (state.value.isSaving) return
        val shifts = shiftsLink.text.toString().trim()
        val invoices = invoicesLink.text.toString().trim()
        val required = UiText.Resource(Res.string.config_link_required)
        state.update {
            it.copy(
                shiftsError = required.takeIf { shifts.isEmpty() },
                invoicesError = required.takeIf { invoices.isEmpty() },
                formError = null,
            )
        }
        if (shifts.isEmpty() || invoices.isEmpty()) return
        state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = saveSettings.save(shifts, invoices)) {
                is Result.Success -> {
                    fillLinks(result.data)
                    state.update { it.copy(isSaving = false, settings = result.data, editing = false) }
                    _uiEvents.emit(GoogleDriveUiEvent.ShowMessage(UiText.Resource(Res.string.config_saved)))
                }
                is Result.Failure -> {
                    val error = result.error
                    state.update {
                        if (error is SheetsError.NotAccessible) {
                            val text = error.toUiText()
                            it.copy(
                                isSaving = false,
                                shiftsError = text.takeIf { error.field == SpreadsheetField.SHIFTS },
                                invoicesError = text.takeIf { error.field == SpreadsheetField.INVOICES },
                            )
                        } else {
                            it.copy(isSaving = false, formError = error.toUiText())
                        }
                    }
                }
            }
        }
    }

    private fun export() {
        if (state.value.export == TaskProgressUiState.Running) return
        val month = state.value.exportMonth
        state.update { it.copy(export = TaskProgressUiState.Running) }
        viewModelScope.launch {
            val progress = run(exportShiftMonth.export(month), TaskTexts.EXPORT, month)
            state.update { it.copy(export = progress) }
            loadStatus()
        }
    }

    private fun resync() {
        if (state.value.resync == TaskProgressUiState.Running) return
        val month = state.value.resyncMonth
        state.update { it.copy(resync = TaskProgressUiState.Running) }
        viewModelScope.launch {
            val progress = run(resyncInvoiceMonth.resync(month), TaskTexts.RESYNC, month)
            state.update { it.copy(resync = progress) }
            loadStatus()
        }
    }

    /** Follows the task the server created until it ends, and says how it went. */
    private suspend fun run(
        created: Result<String, SheetsError>,
        texts: TaskTexts,
        month: SheetMonth,
    ): TaskProgressUiState {
        val taskId =
            when (created) {
                is Result.Failure -> return TaskProgressUiState.Failed(created.error.toUiText())
                is Result.Success -> created.data
            }
        return when (val outcome = awaitTask.await(taskId)) {
            is Result.Failure -> TaskProgressUiState.Failed(outcome.error.toUiText())
            is Result.Success ->
                when (val ended = outcome.data) {
                    is SheetTaskOutcome.StillQueued -> TaskProgressUiState.Queued(UiText.Resource(texts.queued))
                    is SheetTaskOutcome.Finished -> ended.task.toProgress(texts, month)
                }
        }
    }

    private fun SheetTask.toProgress(
        texts: TaskTexts,
        month: SheetMonth,
    ): TaskProgressUiState {
        if (status == SheetTaskStatus.FAILED) return TaskProgressUiState.Failed(reasonText())
        val rows = rowCount ?: 0
        val label = dates.month(month)
        val message =
            if (rows == 1) UiText.Resource(texts.doneOne, arrayOf(label)) else UiText.Resource(texts.done, arrayOf(rows, label))
        val pending = lastNamesPending ?: 0
        val warning =
            when {
                texts != TaskTexts.EXPORT || pending <= 0 -> null
                pending == 1 -> UiText.Resource(Res.string.export_last_names_pending_one)
                else -> UiText.Resource(Res.string.export_last_names_pending, arrayOf(pending))
            }
        return TaskProgressUiState.Done(message, warning)
    }

    private fun retry(taskId: String) {
        if (taskId in state.value.retrying) return
        state.update { it.copy(retrying = it.retrying + taskId) }
        viewModelScope.launch {
            val message =
                when (val result = retryTask.retry(taskId)) {
                    is Result.Success -> UiText.Resource(Res.string.status_retried)
                    is Result.Failure -> result.error.toUiText()
                }
            loadStatus()
            state.update { it.copy(retrying = it.retrying - taskId) }
            _uiEvents.emit(GoogleDriveUiEvent.ShowMessage(message))
        }
    }

    private fun retryAll() {
        if (state.value.retryingAll) return
        state.update { it.copy(retryingAll = true) }
        viewModelScope.launch {
            val message =
                when (val result = retryAllFailed.retryAll()) {
                    is Result.Success ->
                        if (result.data == 1) {
                            UiText.Resource(Res.string.status_retried_all_one)
                        } else {
                            UiText.Resource(Res.string.status_retried_all, arrayOf(result.data))
                        }
                    is Result.Failure -> result.error.toUiText()
                }
            loadStatus()
            state.update { it.copy(retryingAll = false) }
            _uiEvents.emit(GoogleDriveUiEvent.ShowMessage(message))
        }
    }

    private fun GoogleDriveInternalState.toUiState(): GoogleDriveScreenUiState {
        val settings = settings
        return when {
            isLoading -> GoogleDriveScreenUiState.Loading
            loadError != null -> GoogleDriveScreenUiState.Error(loadError.toUiText())
            settings == null -> GoogleDriveScreenUiState.Loading
            !settings.available -> GoogleDriveScreenUiState.Unavailable
            else ->
                GoogleDriveScreenUiState.Loaded(
                    GoogleDriveUiState(
                        serviceAccountEmail = settings.serviceAccountEmail.orEmpty(),
                        config =
                            SheetsConfigUiState(
                                shiftsUrl = settings.shifts?.url,
                                invoicesUrl = settings.invoices?.url,
                                isEditing = editing,
                                shiftsError = shiftsError,
                                invoicesError = invoicesError,
                                formError = formError,
                                isSaving = isSaving,
                            ),
                        months = months.map { it.option() },
                        exportMonth = exportMonth.option(),
                        export = export,
                        resyncMonth = resyncMonth.option(),
                        resync = resync,
                        status = status?.toUiState(retrying, retryingAll),
                        statusError = statusError,
                    ),
                )
        }
    }

    private fun SheetMonth.option() = MonthOptionUiState(this, dates.month(this))

    private fun SheetsStatus.toUiState(
        retrying: Set<String>,
        retryingAll: Boolean,
    ) = SheetsStatusUiState(
        lastSuccessLabel = lastSuccessAt?.let(dates::moment),
        pending = pending,
        failedCount = failed,
        failed =
            failedTasks.map {
                FailedTaskUiState(
                    id = it.id,
                    title = it.titleText(dates),
                    reason = it.reasonText(),
                    attempts = it.attempts,
                    isRetrying = it.id in retrying || retryingAll,
                )
            },
        isRetryingAll = retryingAll,
    )

    /** The texts of each kind of month task. */
    private enum class TaskTexts(
        val done: StringResource,
        val doneOne: StringResource,
        val queued: StringResource,
    ) {
        EXPORT(Res.string.export_done, Res.string.export_done_one, Res.string.export_queued),
        RESYNC(Res.string.resync_done, Res.string.resync_done_one, Res.string.resync_queued),
    }
}
