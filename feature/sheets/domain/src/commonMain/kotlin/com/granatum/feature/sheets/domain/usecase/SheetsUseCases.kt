package com.granatum.feature.sheets.domain.usecase

import com.granatum.core.domain.util.Result
import com.granatum.feature.sheets.domain.model.SheetMonth
import com.granatum.feature.sheets.domain.model.SheetTask
import com.granatum.feature.sheets.domain.model.SheetTaskOutcome
import com.granatum.feature.sheets.domain.model.SheetsError
import com.granatum.feature.sheets.domain.model.SheetsSettings
import com.granatum.feature.sheets.domain.model.SheetsStatus
import com.granatum.feature.sheets.domain.repository.SheetsRepository
import kotlinx.coroutines.delay

class GetSheetsSettingsUseCase(
    private val repository: SheetsRepository,
) {
    suspend fun get(): Result<SheetsSettings, SheetsError> = repository.settings()
}

class SaveSheetsSettingsUseCase(
    private val repository: SheetsRepository,
) {
    suspend fun save(
        shiftsSpreadsheet: String,
        invoicesSpreadsheet: String,
    ): Result<SheetsSettings, SheetsError> = repository.saveSettings(shiftsSpreadsheet.trim(), invoicesSpreadsheet.trim())
}

class ExportShiftMonthUseCase(
    private val repository: SheetsRepository,
) {
    suspend fun export(month: SheetMonth): Result<String, SheetsError> = repository.exportShiftMonth(month)
}

class ResyncInvoiceMonthUseCase(
    private val repository: SheetsRepository,
) {
    suspend fun resync(month: SheetMonth): Result<String, SheetsError> = repository.resyncInvoiceMonth(month)
}

class GetSheetsStatusUseCase(
    private val repository: SheetsRepository,
) {
    suspend fun get(): Result<SheetsStatus, SheetsError> = repository.status()
}

class RetrySheetTaskUseCase(
    private val repository: SheetsRepository,
) {
    suspend fun retry(id: String): Result<SheetTask, SheetsError> = repository.retry(id)
}

class RetryAllFailedSheetTasksUseCase(
    private val repository: SheetsRepository,
) {
    suspend fun retryAll(): Result<Int, SheetsError> = repository.retryAllFailed()
}

/**
 * Follows a task until it is done or failed: a query every [intervalMillis], at most [maxPolls]
 * (2 s and 30 by default, about a minute: SC-002). After that it gives up and answers
 * [SheetTaskOutcome.StillQueued]: the server finishes it when Google answers. [pause] is
 * injectable so tests need not wait.
 */
class AwaitSheetTaskUseCase(
    private val repository: SheetsRepository,
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    private val maxPolls: Int = DEFAULT_MAX_POLLS,
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun await(taskId: String): Result<SheetTaskOutcome, SheetsError> {
        var last: SheetTask? = null
        repeat(maxPolls) { index ->
            if (index > 0) pause(intervalMillis)
            when (val result = repository.task(taskId)) {
                is Result.Failure -> return result
                is Result.Success -> {
                    if (result.data.isFinished) return Result.Success(SheetTaskOutcome.Finished(result.data))
                    last = result.data
                }
            }
        }
        return last?.let { Result.Success(SheetTaskOutcome.StillQueued(it)) } ?: Result.Failure(SheetsError.Unknown)
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 2_000L
        const val DEFAULT_MAX_POLLS = 30
    }
}

/** This month where the business is: the newest month the selector offers. */
fun interface CurrentMonthProvider {
    fun current(): SheetMonth
}
