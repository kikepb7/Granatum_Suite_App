package com.granatum.feature.sheets.domain.model

import com.granatum.core.domain.util.Error

/** The backend's error codes for `/api/sheets` (backend specs/012-google-sheets-sync/contracts). */
sealed interface SheetsError : Error {
    /** `422 SPREADSHEET_NOT_ACCESSIBLE`: the technical account cannot edit [field]. */
    data class NotAccessible(
        val field: SpreadsheetField,
        val reason: SheetAccessProblem,
    ) : SheetsError

    /** `400 VALIDATION`: a link that is neither a spreadsheet link nor an id. */
    data object InvalidLink : SheetsError

    /** `409 SAME_SPREADSHEET`. */
    data object SameSpreadsheet : SheetsError

    /** `409 SHEETS_NOT_CONFIGURED`: the spreadsheet for that export is missing. */
    data object NotConfigured : SheetsError

    /** `503 SHEETS_UNAVAILABLE`: the server has no Google credentials. */
    data object Unavailable : SheetsError

    /** `502 SHEETS_UNREACHABLE`: Google did not answer; nothing was saved. */
    data object Unreachable : SheetsError

    /** `400 INVALID_PERIOD`: a month that does not exist, is in the future or out of retention. */
    data object InvalidPeriod : SheetsError

    /** `404 TASK_NOT_FOUND`. */
    data object TaskNotFound : SheetsError

    /** `409 TASK_NOT_FAILED`: only failed tasks can be retried. */
    data object TaskNotFailed : SheetsError

    data object NoInternet : SheetsError

    data object Forbidden : SheetsError

    data object Unknown : SheetsError
}
