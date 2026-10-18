package com.granatum.feature.sheets.data.repository

import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.map
import com.granatum.feature.sheets.data.dto.SheetQueueStatusDto
import com.granatum.feature.sheets.data.dto.SheetSettingsDto
import com.granatum.feature.sheets.data.dto.SheetTaskDto
import com.granatum.feature.sheets.data.dto.UpdateSheetSettingsRequestDto
import com.granatum.feature.sheets.data.remote.SheetsRemoteDataSource
import com.granatum.feature.sheets.domain.model.SheetMonth
import com.granatum.feature.sheets.domain.model.SheetTask
import com.granatum.feature.sheets.domain.model.SheetTaskKind
import com.granatum.feature.sheets.domain.model.SheetTaskStatus
import com.granatum.feature.sheets.domain.model.SheetsError
import com.granatum.feature.sheets.domain.model.SheetsSettings
import com.granatum.feature.sheets.domain.model.SheetsStatus
import com.granatum.feature.sheets.domain.model.Spreadsheet
import com.granatum.feature.sheets.domain.repository.SheetsRepository
import kotlin.time.Instant

internal class SheetsRepositoryImpl(
    private val remote: SheetsRemoteDataSource,
) : SheetsRepository {
    override suspend fun settings(): Result<SheetsSettings, SheetsError> = remote.settings().map { it.toDomain() }

    override suspend fun saveSettings(
        shiftsSpreadsheet: String,
        invoicesSpreadsheet: String,
    ): Result<SheetsSettings, SheetsError> =
        remote.saveSettings(UpdateSheetSettingsRequestDto(shiftsSpreadsheet, invoicesSpreadsheet)).map { it.toDomain() }

    override suspend fun exportShiftMonth(month: SheetMonth): Result<String, SheetsError> =
        remote.exportShiftMonth(month.year, month.month).map { it.taskId }

    override suspend fun resyncInvoiceMonth(month: SheetMonth): Result<String, SheetsError> =
        remote.resyncInvoiceMonth(month.year, month.month).map { it.taskId }

    override suspend fun task(id: String): Result<SheetTask, SheetsError> = remote.task(id).map { it.toDomain() }

    override suspend fun status(): Result<SheetsStatus, SheetsError> = remote.status().map { it.toDomain() }

    override suspend fun retry(id: String): Result<SheetTask, SheetsError> = remote.retry(id).map { it.toDomain() }

    override suspend fun retryAllFailed(): Result<Int, SheetsError> = remote.retryAllFailed().map { it.requeued }

    private fun SheetSettingsDto.toDomain() =
        SheetsSettings(
            available = available,
            serviceAccountEmail = serviceAccountEmail,
            shifts = spreadsheet(shiftsSpreadsheetId, shiftsSpreadsheetUrl),
            invoices = spreadsheet(invoicesSpreadsheetId, invoicesSpreadsheetUrl),
            configuredAt = configuredAt.toInstant(),
            configuredBy = configuredBy,
        )

    private fun spreadsheet(
        id: String?,
        url: String?,
    ): Spreadsheet? = id?.takeIf { it.isNotBlank() }?.let { Spreadsheet(it, url ?: "https://docs.google.com/spreadsheets/d/$it/edit") }

    private fun SheetTaskDto.toDomain() =
        SheetTask(
            id = id,
            kind = SheetTaskKind.entries.firstOrNull { it.name == kind } ?: SheetTaskKind.UNKNOWN,
            reference = reference,
            status = SheetTaskStatus.entries.firstOrNull { it.name == status } ?: SheetTaskStatus.UNKNOWN,
            attempts = attempts,
            rowCount = rowCount,
            lastNamesPending = lastNamesPending,
            lastError = lastError,
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant(),
            completedAt = completedAt.toInstant(),
        )

    private fun SheetQueueStatusDto.toDomain() =
        SheetsStatus(
            lastSuccessAt = lastSuccessAt.toInstant(),
            pending = pending,
            failed = failed,
            recent = recent.map { it.toDomain() },
        )

    private fun String?.toInstant(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }
}
