package com.granatum.feature.sheets.presentation.drive

import androidx.compose.runtime.Immutable
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.sheets.domain.model.SheetMonth

@Immutable
internal sealed interface GoogleDriveScreenUiState {
    data object Loading : GoogleDriveScreenUiState

    /** The settings could not be read: the usual error with a retry. */
    data class Error(
        val message: UiText,
    ) : GoogleDriveScreenUiState

    /** The server has no Google credentials: that message and nothing else (US2, scenario 2). */
    data object Unavailable : GoogleDriveScreenUiState

    data class Loaded(
        val data: GoogleDriveUiState,
    ) : GoogleDriveScreenUiState
}

@Immutable
internal data class GoogleDriveUiState(
    val serviceAccountEmail: String,
    val config: SheetsConfigUiState,
    val months: List<MonthOptionUiState>,
    val exportMonth: MonthOptionUiState,
    val export: TaskProgressUiState,
    val resyncMonth: MonthOptionUiState,
    val resync: TaskProgressUiState,
    val status: SheetsStatusUiState?,
    val statusError: UiText?,
)

@Immutable
internal data class SheetsConfigUiState(
    /** Links of the saved spreadsheets, to open them in the browser; null until saved. */
    val shiftsUrl: String?,
    val invoicesUrl: String?,
    /** The link fields are shown: nothing saved yet, or the `ADMIN` is changing them. */
    val isEditing: Boolean,
    val shiftsError: UiText?,
    val invoicesError: UiText?,
    /** An error about both: the same spreadsheet twice, Google not answering… */
    val formError: UiText?,
    val isSaving: Boolean,
) {
    val isConfigured: Boolean get() = shiftsUrl != null && invoicesUrl != null
}

@Immutable
internal data class MonthOptionUiState(
    val month: SheetMonth,
    /** `octubre de 2026` */
    val label: String,
)

/** Where an export or a resync is: idle, writing, done, still queued or failed. */
@Immutable
internal sealed interface TaskProgressUiState {
    data object Idle : TaskProgressUiState

    data object Running : TaskProgressUiState

    data class Done(
        val message: UiText,
        /** People without last names, when there are any. */
        val warning: UiText?,
    ) : TaskProgressUiState

    data class Queued(
        val message: UiText,
    ) : TaskProgressUiState

    data class Failed(
        val message: UiText,
    ) : TaskProgressUiState
}

@Immutable
internal data class SheetsStatusUiState(
    /** `18/10/2026 12:05`, or null when nothing has been copied yet. */
    val lastSuccessLabel: String?,
    val pending: Long,
    val failedCount: Long,
    val failed: List<FailedTaskUiState>,
    val isRetryingAll: Boolean,
)

@Immutable
internal data class FailedTaskUiState(
    val id: String,
    val title: UiText,
    val reason: UiText,
    val attempts: Int,
    val isRetrying: Boolean,
)

@Immutable
internal sealed interface GoogleDriveUiAction {
    data object Retry : GoogleDriveUiAction

    data object CopyEmail : GoogleDriveUiAction

    data object StartEditing : GoogleDriveUiAction

    data object CancelEditing : GoogleDriveUiAction

    data object Save : GoogleDriveUiAction

    data class SelectExportMonth(
        val month: SheetMonth,
    ) : GoogleDriveUiAction

    data object Export : GoogleDriveUiAction

    data class SelectResyncMonth(
        val month: SheetMonth,
    ) : GoogleDriveUiAction

    data object Resync : GoogleDriveUiAction

    data object RefreshStatus : GoogleDriveUiAction

    data class RetryTask(
        val taskId: String,
    ) : GoogleDriveUiAction

    data object RetryAllFailed : GoogleDriveUiAction
}

internal sealed interface GoogleDriveUiEvent {
    /** The screen puts [text] on the clipboard and says so. */
    data class CopyToClipboard(
        val text: String,
    ) : GoogleDriveUiEvent

    data class ShowMessage(
        val message: UiText,
    ) : GoogleDriveUiEvent
}
