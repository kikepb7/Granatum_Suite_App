package com.granatum.feature.sheets.domain

import com.granatum.core.domain.util.Result
import com.granatum.feature.sheets.domain.model.SheetFailureReason
import com.granatum.feature.sheets.domain.model.SheetMonth
import com.granatum.feature.sheets.domain.model.SheetTask
import com.granatum.feature.sheets.domain.model.SheetTaskKind
import com.granatum.feature.sheets.domain.model.SheetTaskOutcome
import com.granatum.feature.sheets.domain.model.SheetTaskStatus
import com.granatum.feature.sheets.domain.model.SheetsError
import com.granatum.feature.sheets.domain.model.SheetsSettings
import com.granatum.feature.sheets.domain.model.SheetsStatus
import com.granatum.feature.sheets.domain.repository.SheetsRepository
import com.granatum.feature.sheets.domain.usecase.AwaitSheetTaskUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SheetsDomainTest {
    /** Answers the queued statuses one per query; the last one repeats. */
    private class ScriptedRepository(
        private val answers: List<Result<SheetTask, SheetsError>>,
    ) : SheetsRepository {
        var queries = 0

        override suspend fun task(id: String): Result<SheetTask, SheetsError> = answers[minOf(queries++, answers.lastIndex)]

        override suspend fun settings(): Result<SheetsSettings, SheetsError> = error("unused")

        override suspend fun saveSettings(
            shiftsSpreadsheet: String,
            invoicesSpreadsheet: String,
        ): Result<SheetsSettings, SheetsError> = error("unused")

        override suspend fun exportShiftMonth(month: SheetMonth): Result<String, SheetsError> = error("unused")

        override suspend fun resyncInvoiceMonth(month: SheetMonth): Result<String, SheetsError> = error("unused")

        override suspend fun status(): Result<SheetsStatus, SheetsError> = error("unused")

        override suspend fun retry(id: String): Result<SheetTask, SheetsError> = error("unused")

        override suspend fun retryAllFailed(): Result<Int, SheetsError> = error("unused")
    }

    private fun task(
        status: SheetTaskStatus,
        rows: Int? = null,
    ) = SheetTask("t1", SheetTaskKind.SHIFT_MONTH, "2026-10", status, 1, rows, rows?.let { 1 }, null, null, null, null)

    private val pauses = mutableListOf<Long>()

    private fun sut(repository: SheetsRepository) = AwaitSheetTaskUseCase(repository, pause = { pauses += it })

    @Test
    fun `When the task finishes after a few queries then the done task comes back`() =
        runTest {
            val repository =
                ScriptedRepository(
                    listOf(
                        Result.Success(task(SheetTaskStatus.PENDING)),
                        Result.Success(task(SheetTaskStatus.RUNNING)),
                        Result.Success(task(SheetTaskStatus.DONE, rows = 92)),
                    ),
                )

            val actual = sut(repository).await("t1")

            val outcome = assertIs<SheetTaskOutcome.Finished>(assertIs<Result.Success<SheetTaskOutcome>>(actual).data)
            assertEquals(92, outcome.task.rowCount)
            assertEquals(3, repository.queries)
            assertEquals(listOf(2_000L, 2_000L), pauses)
        }

    @Test
    fun `When the task fails then waiting stops with the failed task`() =
        runTest {
            val repository = ScriptedRepository(listOf(Result.Success(task(SheetTaskStatus.FAILED))))

            val actual = sut(repository).await("t1")

            val outcome = assertIs<SheetTaskOutcome.Finished>(assertIs<Result.Success<SheetTaskOutcome>>(actual).data)
            assertEquals(SheetTaskStatus.FAILED, outcome.task.status)
            assertEquals(1, repository.queries)
        }

    @Test
    fun `When the task is still pending after thirty queries then it is still queued`() =
        runTest {
            val repository = ScriptedRepository(listOf(Result.Success(task(SheetTaskStatus.PENDING))))

            val actual = sut(repository).await("t1")

            assertIs<SheetTaskOutcome.StillQueued>(assertIs<Result.Success<SheetTaskOutcome>>(actual).data)
            assertEquals(30, repository.queries)
            assertEquals(29, pauses.size)
        }

    @Test
    fun `When a query fails then waiting stops with that error`() =
        runTest {
            val repository = ScriptedRepository(listOf(Result.Failure(SheetsError.TaskNotFound)))

            val actual = sut(repository).await("t1")

            assertEquals(SheetsError.TaskNotFound, assertIs<Result.Failure<SheetsError>>(actual).error)
        }

    @Test
    fun `The selector starts at the current month and goes back without future months`() {
        val actual = SheetMonth.exportable(SheetMonth(2026, 2), count = 4)

        assertEquals(listOf(SheetMonth(2026, 2), SheetMonth(2026, 1), SheetMonth(2025, 12), SheetMonth(2025, 11)), actual)
        assertEquals(48, SheetMonth.exportable(SheetMonth(2026, 10)).size)
        assertEquals("2026-03", SheetMonth(2026, 3).toString())
        assertEquals(SheetMonth(2026, 3), SheetMonth.parse("2026-03"))
        assertNull(SheetMonth.parse("abc"))
    }

    @Test
    fun `The server's failure messages are recognised`() {
        assertEquals(
            SheetFailureReason.READ_ONLY,
            SheetFailureReason.from("La hoja está compartida con la cuenta técnica de Google solo como lectora"),
        )
        assertEquals(SheetFailureReason.NOT_SHARED, SheetFailureReason.from("La hoja no está compartida con la cuenta técnica de Google"))
        assertEquals(SheetFailureReason.NOT_FOUND, SheetFailureReason.from("La hoja no existe o la cuenta técnica de Google no la ve"))
        assertEquals(SheetFailureReason.TRANSIENT, SheetFailureReason.from("Error inesperado al escribir en la hoja; se reintentará"))
        assertEquals(SheetFailureReason.NO_HANDLER, SheetFailureReason.from("No hay ningún escritor para las tareas de tipo INVOICE"))
        assertEquals(SheetFailureReason.OTHER, SheetFailureReason.from("Algo raro"))
        assertNull(SheetFailureReason.from(null))
    }

    @Test
    fun `When the server sends an error code then it is used and unknown codes are other`() {
        assertEquals(SheetFailureReason.API_DISABLED, SheetFailureReason.fromCode("API_DISABLED"))
        assertEquals(SheetFailureReason.NO_HANDLER, SheetFailureReason.fromCode("NO_HANDLER"))
        assertEquals(SheetFailureReason.OTHER, SheetFailureReason.fromCode("SOMETHING_NEW"))
        assertEquals(null, SheetFailureReason.fromCode(null))
    }
}
