package com.granatum.feature.sheets.domain.repository

import com.granatum.core.domain.util.Result
import com.granatum.feature.sheets.domain.model.SheetMonth
import com.granatum.feature.sheets.domain.model.SheetTask
import com.granatum.feature.sheets.domain.model.SheetsError
import com.granatum.feature.sheets.domain.model.SheetsSettings
import com.granatum.feature.sheets.domain.model.SheetsStatus

/**
 * Straight to the server, nothing kept on the device: the queue changes by itself on the server,
 * so a cached copy would show a false state (plan, Complexity Tracking).
 */
interface SheetsRepository {
    suspend fun settings(): Result<SheetsSettings, SheetsError>

    /** Each value is a spreadsheet link or its bare id. */
    suspend fun saveSettings(
        shiftsSpreadsheet: String,
        invoicesSpreadsheet: String,
    ): Result<SheetsSettings, SheetsError>

    /** The id of the task that rewrites the month's shifts tab. */
    suspend fun exportShiftMonth(month: SheetMonth): Result<String, SheetsError>

    /** The id of the task that rewrites the month's receipts tab. */
    suspend fun resyncInvoiceMonth(month: SheetMonth): Result<String, SheetsError>

    suspend fun task(id: String): Result<SheetTask, SheetsError>

    suspend fun status(): Result<SheetsStatus, SheetsError>

    suspend fun retry(id: String): Result<SheetTask, SheetsError>

    /** How many failed tasks went back to the queue. */
    suspend fun retryAllFailed(): Result<Int, SheetsError>
}
