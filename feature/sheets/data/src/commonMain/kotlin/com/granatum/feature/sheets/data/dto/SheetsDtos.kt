package com.granatum.feature.sheets.data.dto

import kotlinx.serialization.Serializable

// Field for field with the backend spec 012 contract (contracts/README.md) and its SheetsDtos.kt;
// docs/openapi.json is not pinned to it yet. Defaults keep the parsing tolerant.

/** `GET` and `PUT /api/sheets/settings`. */
@Serializable
internal data class SheetSettingsDto(
    val available: Boolean = false,
    val serviceAccountEmail: String? = null,
    val shiftsSpreadsheetId: String? = null,
    val shiftsSpreadsheetUrl: String? = null,
    val invoicesSpreadsheetId: String? = null,
    val invoicesSpreadsheetUrl: String? = null,
    val configuredAt: String? = null,
    val configuredBy: String? = null,
)

/** `PUT /api/sheets/settings`: a link or a bare id for each. */
@Serializable
internal data class UpdateSheetSettingsRequestDto(
    val shiftsSpreadsheet: String,
    val invoicesSpreadsheet: String,
)

/** `202` of `POST /api/sheets/shifts/{year}/{month}` and `/invoices/{year}/{month}/resync`. */
@Serializable
internal data class SheetTaskCreatedDto(
    val taskId: String,
)

/** `GET /api/sheets/tasks/{id}` and the items of the status. */
@Serializable
internal data class SheetTaskDto(
    val id: String,
    val kind: String = "",
    val reference: String = "",
    val status: String = "",
    val attempts: Int = 0,
    val rowCount: Int? = null,
    val lastNamesPending: Int? = null,
    val lastError: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val completedAt: String? = null,
)

/** `GET /api/sheets/status`. */
@Serializable
internal data class SheetQueueStatusDto(
    val lastSuccessAt: String? = null,
    val pending: Long = 0,
    val failed: Long = 0,
    val recent: List<SheetTaskDto> = emptyList(),
)

/** `POST /api/sheets/tasks/retry-failed`. */
@Serializable
internal data class SheetRetryAllDto(
    val requeued: Int = 0,
)

/** `{code, message}`, plus `spreadsheet` and `reason` on `422 SPREADSHEET_NOT_ACCESSIBLE`. */
@Serializable
internal data class SheetsErrorBodyDto(
    val code: String? = null,
    val message: String? = null,
    val spreadsheet: String? = null,
    val reason: String? = null,
)
