package com.granatum.feature.sheets.presentation

import com.granatum.core.domain.util.Result
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

internal val EMAIL = "granatum-demo@demo.iam.gserviceaccount.com"

internal fun sheetsSettings(
    available: Boolean = true,
    configured: Boolean = true,
) = SheetsSettings(
    available = available,
    serviceAccountEmail = EMAIL.takeIf { available },
    shifts = Spreadsheet("1AbC", "https://docs.google.com/spreadsheets/d/1AbC/edit").takeIf { configured && available },
    invoices = Spreadsheet("1XyZ", "https://docs.google.com/spreadsheets/d/1XyZ/edit").takeIf { configured && available },
    configuredAt = null,
    configuredBy = null,
)

internal fun task(
    id: String = "t1",
    status: SheetTaskStatus = SheetTaskStatus.DONE,
    kind: SheetTaskKind = SheetTaskKind.SHIFT_MONTH,
    reference: String = "2026-10",
    rows: Int? = 92,
    pendingLastNames: Int? = 0,
    error: String? = null,
) = SheetTask(id, kind, reference, status, 1, rows, pendingLastNames, error, null, null, null)

internal class FakeSheetsRepository : SheetsRepository {
    var settingsResult: Result<SheetsSettings, SheetsError> = Result.Success(sheetsSettings())
    var saveResult: Result<SheetsSettings, SheetsError> = Result.Success(sheetsSettings())
    var exportResult: Result<String, SheetsError> = Result.Success("t1")
    var resyncResult: Result<String, SheetsError> = Result.Success("t2")

    /** Answers of `task`, one per query; the last one repeats. */
    var taskAnswers: List<Result<SheetTask, SheetsError>> = listOf(Result.Success(task()))
    var statusResult: Result<SheetsStatus, SheetsError> =
        Result.Success(SheetsStatus(Instant.parse("2026-10-18T10:05:00Z"), 0, 0, emptyList()))
    var retryResult: Result<SheetTask, SheetsError> = Result.Success(task(status = SheetTaskStatus.PENDING))
    var retryAllResult: Result<Int, SheetsError> = Result.Success(3)

    val saved = mutableListOf<Pair<String, String>>()
    val exported = mutableListOf<SheetMonth>()
    val resynced = mutableListOf<SheetMonth>()
    val retried = mutableListOf<String>()
    var statusQueries = 0
    private var taskQueries = 0

    override suspend fun settings() = settingsResult

    override suspend fun saveSettings(
        shiftsSpreadsheet: String,
        invoicesSpreadsheet: String,
    ): Result<SheetsSettings, SheetsError> {
        saved += shiftsSpreadsheet to invoicesSpreadsheet
        return saveResult
    }

    override suspend fun exportShiftMonth(month: SheetMonth): Result<String, SheetsError> {
        exported += month
        return exportResult
    }

    override suspend fun resyncInvoiceMonth(month: SheetMonth): Result<String, SheetsError> {
        resynced += month
        return resyncResult
    }

    override suspend fun task(id: String) = taskAnswers[minOf(taskQueries++, taskAnswers.lastIndex)]

    override suspend fun status(): Result<SheetsStatus, SheetsError> {
        statusQueries++
        return statusResult
    }

    override suspend fun retry(id: String): Result<SheetTask, SheetsError> {
        retried += id
        return retryResult
    }

    override suspend fun retryAllFailed() = retryAllResult
}
