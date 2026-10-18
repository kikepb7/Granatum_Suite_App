package com.granatum.feature.sheets.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.sheets.domain.model.SheetAccessProblem
import com.granatum.feature.sheets.domain.model.SheetMonth
import com.granatum.feature.sheets.domain.model.SheetTaskKind
import com.granatum.feature.sheets.domain.model.SheetTaskStatus
import com.granatum.feature.sheets.domain.model.SheetsError
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
import com.granatum.feature.sheets.presentation.drive.GoogleDriveScreenUiState
import com.granatum.feature.sheets.presentation.drive.GoogleDriveUiAction
import com.granatum.feature.sheets.presentation.drive.GoogleDriveUiEvent
import com.granatum.feature.sheets.presentation.drive.GoogleDriveUiState
import com.granatum.feature.sheets.presentation.drive.GoogleDriveViewModel
import com.granatum.feature.sheets.presentation.drive.TaskProgressUiState
import granatumsuite.feature.sheets.presentation.generated.resources.Res
import granatumsuite.feature.sheets.presentation.generated.resources.config_link_required
import granatumsuite.feature.sheets.presentation.generated.resources.config_saved
import granatumsuite.feature.sheets.presentation.generated.resources.error_not_configured
import granatumsuite.feature.sheets.presentation.generated.resources.error_same_spreadsheet
import granatumsuite.feature.sheets.presentation.generated.resources.export_done
import granatumsuite.feature.sheets.presentation.generated.resources.export_last_names_pending
import granatumsuite.feature.sheets.presentation.generated.resources.export_queued
import granatumsuite.feature.sheets.presentation.generated.resources.field_not_shared
import granatumsuite.feature.sheets.presentation.generated.resources.field_read_only
import granatumsuite.feature.sheets.presentation.generated.resources.reason_read_only_invoices
import granatumsuite.feature.sheets.presentation.generated.resources.reason_transient
import granatumsuite.feature.sheets.presentation.generated.resources.resync_done
import granatumsuite.feature.sheets.presentation.generated.resources.status_retried
import granatumsuite.feature.sheets.presentation.generated.resources.status_retried_all
import granatumsuite.feature.sheets.presentation.generated.resources.task_invoice_month
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleDriveViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repository = FakeSheetsRepository()
    private val october = SheetMonth(2026, 10)

    private fun sut() =
        GoogleDriveViewModel(
            getSettings = GetSheetsSettingsUseCase(repository),
            saveSettings = SaveSheetsSettingsUseCase(repository),
            exportShiftMonth = ExportShiftMonthUseCase(repository),
            resyncInvoiceMonth = ResyncInvoiceMonthUseCase(repository),
            awaitTask = AwaitSheetTaskUseCase(repository, pause = {}),
            getStatus = GetSheetsStatusUseCase(repository),
            retryTask = RetrySheetTaskUseCase(repository),
            retryAllFailed = RetryAllFailedSheetTasksUseCase(repository),
            currentMonth = CurrentMonthProvider { october },
            dates = SheetsDateFormatter(TimeZone.UTC),
        )

    private fun GoogleDriveViewModel.loaded(): GoogleDriveUiState = assertIs<GoogleDriveScreenUiState.Loaded>(uiState.value).data

    // --- Carga

    @Test
    fun `When the server has no Google credentials then the screen says it is unavailable and nothing else`() =
        runTest(dispatcher) {
            repository.settingsResult = Result.Success(sheetsSettings(available = false))

            val sut = sut()

            assertEquals(GoogleDriveScreenUiState.Unavailable, sut.uiState.value)
            assertEquals(0, repository.statusQueries)
        }

    @Test
    fun `When no spreadsheets are saved yet then the link fields are shown empty`() =
        runTest(dispatcher) {
            repository.settingsResult = Result.Success(sheetsSettings(configured = false))

            val sut = sut()

            val actual = sut.loaded()
            assertEquals(EMAIL, actual.serviceAccountEmail)
            assertTrue(actual.config.isEditing)
            assertFalse(actual.config.isConfigured)
            assertEquals("", sut.shiftsLink.text.toString())
        }

    @Test
    fun `When the spreadsheets are configured then their links can be opened and the status is shown`() =
        runTest(dispatcher) {
            val sut = sut()

            val actual = sut.loaded()
            assertFalse(actual.config.isEditing)
            assertEquals("https://docs.google.com/spreadsheets/d/1AbC/edit", actual.config.shiftsUrl)
            assertEquals("https://docs.google.com/spreadsheets/d/1XyZ/edit", actual.config.invoicesUrl)
            assertEquals("18/10/2026 10:05", actual.status?.lastSuccessLabel)
        }

    @Test
    fun `When the settings cannot be read then the error is shown and retrying loads them`() =
        runTest(dispatcher) {
            repository.settingsResult = Result.Failure(SheetsError.NoInternet)
            val sut = sut()
            assertIs<GoogleDriveScreenUiState.Error>(sut.uiState.value)

            repository.settingsResult = Result.Success(sheetsSettings())
            sut.onAction(GoogleDriveUiAction.Retry)

            assertIs<GoogleDriveScreenUiState.Loaded>(sut.uiState.value)
        }

    // --- Guardar

    @Test
    fun `When both links are saved then the settings are confirmed`() =
        runTest(dispatcher) {
            repository.settingsResult = Result.Success(sheetsSettings(configured = false))
            val sut = sut()
            sut.shiftsLink.setTextAndPlaceCursorAtEnd(" https://docs.google.com/spreadsheets/d/1AbC/edit ")
            sut.invoicesLink.setTextAndPlaceCursorAtEnd("1XyZ")

            sut.uiEvents.test {
                sut.onAction(GoogleDriveUiAction.Save)

                assertEquals(GoogleDriveUiEvent.ShowMessage(UiText.Resource(Res.string.config_saved)), awaitItem())
            }
            assertEquals(listOf("https://docs.google.com/spreadsheets/d/1AbC/edit" to "1XyZ"), repository.saved)
            val actual = sut.loaded().config
            assertFalse(actual.isEditing)
            assertTrue(actual.isConfigured)
        }

    @Test
    fun `When a link is empty then nothing is sent and the field says so`() =
        runTest(dispatcher) {
            repository.settingsResult = Result.Success(sheetsSettings(configured = false))
            val sut = sut()
            sut.shiftsLink.setTextAndPlaceCursorAtEnd("1AbC")

            sut.onAction(GoogleDriveUiAction.Save)

            assertTrue(repository.saved.isEmpty())
            assertEquals(UiText.Resource(Res.string.config_link_required), sut.loaded().config.invoicesError)
            assertNull(sut.loaded().config.shiftsError)
        }

    @Test
    fun `When the receipts spreadsheet is read only then the error is on its field`() =
        runTest(dispatcher) {
            repository.settingsResult = Result.Success(sheetsSettings(configured = false))
            repository.saveResult = Result.Failure(SheetsError.NotAccessible(SpreadsheetField.INVOICES, SheetAccessProblem.READ_ONLY))
            val sut = sut()
            sut.shiftsLink.setTextAndPlaceCursorAtEnd("1AbC")
            sut.invoicesLink.setTextAndPlaceCursorAtEnd("solo-lectura")

            sut.onAction(GoogleDriveUiAction.Save)

            val actual = sut.loaded().config
            assertEquals(UiText.Resource(Res.string.field_read_only), actual.invoicesError)
            assertNull(actual.shiftsError)
            assertTrue(actual.isEditing)
            assertFalse(actual.isSaving)
        }

    @Test
    fun `When the shifts spreadsheet is not shared then the error is on its field`() =
        runTest(dispatcher) {
            repository.settingsResult = Result.Success(sheetsSettings(configured = false))
            repository.saveResult = Result.Failure(SheetsError.NotAccessible(SpreadsheetField.SHIFTS, SheetAccessProblem.NOT_SHARED))
            val sut = sut()
            sut.shiftsLink.setTextAndPlaceCursorAtEnd("no-compartida")
            sut.invoicesLink.setTextAndPlaceCursorAtEnd("1XyZ")

            sut.onAction(GoogleDriveUiAction.Save)

            assertEquals(UiText.Resource(Res.string.field_not_shared), sut.loaded().config.shiftsError)
        }

    @Test
    fun `When both links point to the same spreadsheet then the form says they must differ`() =
        runTest(dispatcher) {
            repository.settingsResult = Result.Success(sheetsSettings(configured = false))
            repository.saveResult = Result.Failure(SheetsError.SameSpreadsheet)
            val sut = sut()
            sut.shiftsLink.setTextAndPlaceCursorAtEnd("1AbC")
            sut.invoicesLink.setTextAndPlaceCursorAtEnd("1AbC")

            sut.onAction(GoogleDriveUiAction.Save)

            assertEquals(UiText.Resource(Res.string.error_same_spreadsheet), sut.loaded().config.formError)
        }

    @Test
    fun `When the spreadsheets are changed then the fields start with the saved links`() =
        runTest(dispatcher) {
            val sut = sut()

            sut.onAction(GoogleDriveUiAction.StartEditing)

            assertTrue(sut.loaded().config.isEditing)
            assertEquals("https://docs.google.com/spreadsheets/d/1XyZ/edit", sut.invoicesLink.text.toString())
            sut.onAction(GoogleDriveUiAction.CancelEditing)
            assertFalse(sut.loaded().config.isEditing)
        }

    // --- Copiar el correo

    @Test
    fun `When the email is copied then the screen is asked to put it on the clipboard`() =
        runTest(dispatcher) {
            val sut = sut()

            sut.uiEvents.test {
                sut.onAction(GoogleDriveUiAction.CopyEmail)

                assertEquals(GoogleDriveUiEvent.CopyToClipboard(EMAIL), awaitItem())
            }
        }

    // --- Exportar

    @Test
    fun `When a month is exported then progress ends in the rows written and the pending last names`() =
        runTest(dispatcher) {
            repository.taskAnswers =
                listOf(
                    Result.Success(task(status = SheetTaskStatus.PENDING)),
                    Result.Success(task(status = SheetTaskStatus.DONE, rows = 92, pendingLastNames = 2)),
                )
            val sut = sut()

            sut.uiState.test {
                skipItems(1)
                sut.onAction(GoogleDriveUiAction.Export)

                // Unconfined: the whole wait may run before the first emission is seen.
                var item = assertIs<GoogleDriveScreenUiState.Loaded>(awaitItem()).data.export
                if (item == TaskProgressUiState.Running) item = assertIs<GoogleDriveScreenUiState.Loaded>(awaitItem()).data.export
                val done = assertIs<TaskProgressUiState.Done>(item)
                assertEquals(UiText.Resource(Res.string.export_done, arrayOf(92, "octubre de 2026")), done.message)
                assertEquals(UiText.Resource(Res.string.export_last_names_pending, arrayOf(2)), done.warning)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf(october), repository.exported)
        }

    @Test
    fun `While the export runs the screen shows its progress`() =
        runTest(dispatcher) {
            val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
            val sut =
                GoogleDriveViewModel(
                    GetSheetsSettingsUseCase(repository),
                    SaveSheetsSettingsUseCase(repository),
                    ExportShiftMonthUseCase(repository),
                    ResyncInvoiceMonthUseCase(repository),
                    AwaitSheetTaskUseCase(repository, pause = { gate.await() }),
                    GetSheetsStatusUseCase(repository),
                    RetrySheetTaskUseCase(repository),
                    RetryAllFailedSheetTasksUseCase(repository),
                    CurrentMonthProvider { october },
                    SheetsDateFormatter(TimeZone.UTC),
                )
            repository.taskAnswers = listOf(Result.Success(task(status = SheetTaskStatus.PENDING)), Result.Success(task()))

            sut.onAction(GoogleDriveUiAction.Export)
            assertEquals(TaskProgressUiState.Running, sut.loaded().export)

            gate.complete(Unit)
            assertIs<TaskProgressUiState.Done>(sut.loaded().export)
            assertNull((sut.loaded().export as TaskProgressUiState.Done).warning)
        }

    @Test
    fun `When Google does not answer in time then the export is still queued`() =
        runTest(dispatcher) {
            repository.taskAnswers = listOf(Result.Success(task(status = SheetTaskStatus.PENDING)))
            val sut = sut()

            sut.onAction(GoogleDriveUiAction.Export)

            assertEquals(TaskProgressUiState.Queued(UiText.Resource(Res.string.export_queued)), sut.loaded().export)
        }

    @Test
    fun `When the shifts spreadsheet is not configured then the export asks to configure it first`() =
        runTest(dispatcher) {
            repository.exportResult = Result.Failure(SheetsError.NotConfigured)
            val sut = sut()

            sut.onAction(GoogleDriveUiAction.Export)

            assertEquals(TaskProgressUiState.Failed(UiText.Resource(Res.string.error_not_configured)), sut.loaded().export)
        }

    @Test
    fun `When the export fails on the server then the reason is translated`() =
        runTest(dispatcher) {
            repository.taskAnswers =
                listOf(
                    Result.Success(
                        task(status = SheetTaskStatus.FAILED, error = "Error inesperado al escribir en la hoja; se reintentará"),
                    ),
                )
            val sut = sut()

            sut.onAction(GoogleDriveUiAction.Export)

            assertEquals(TaskProgressUiState.Failed(UiText.Resource(Res.string.reason_transient)), sut.loaded().export)
        }

    // --- Meses

    @Test
    fun `The month selector starts at the current month and offers no future months`() =
        runTest(dispatcher) {
            val sut = sut()

            val actual = sut.loaded()
            assertEquals(october, actual.months.first().month)
            assertEquals("octubre de 2026", actual.months.first().label)
            assertTrue(actual.months.all { it.month <= october })
            assertEquals(october, actual.exportMonth.month)

            sut.onAction(GoogleDriveUiAction.SelectExportMonth(SheetMonth(2026, 11)))
            assertEquals(october, sut.loaded().exportMonth.month)

            sut.onAction(GoogleDriveUiAction.SelectExportMonth(SheetMonth(2026, 9)))
            assertEquals(SheetMonth(2026, 9), sut.loaded().exportMonth.month)
        }

    // --- Estado y reintentos (US4)

    private val failedStatus =
        SheetsStatus(
            lastSuccessAt = null,
            pending = 1,
            failed = 2,
            recent =
                listOf(
                    task(
                        id = "f1",
                        status = SheetTaskStatus.FAILED,
                        kind = SheetTaskKind.INVOICE_MONTH,
                        reference = "2026-09",
                        error = "La hoja está compartida con la cuenta técnica de Google solo como lectora",
                    ),
                    task(id = "f2", status = SheetTaskStatus.FAILED, error = "Google no responde"),
                    task(id = "p1", status = SheetTaskStatus.PENDING),
                ),
        )

    @Test
    fun `The status shows the failed tasks with a translated reason`() =
        runTest(dispatcher) {
            repository.statusResult = Result.Success(failedStatus)

            val actual = sut().loaded().status!!

            assertNull(actual.lastSuccessLabel)
            assertEquals(1, actual.pending)
            assertEquals(2, actual.failedCount)
            assertEquals(listOf("f1", "f2"), actual.failed.map { it.id })
            assertEquals(UiText.Resource(Res.string.reason_read_only_invoices), actual.failed[0].reason)
            assertEquals(UiText.Resource(Res.string.task_invoice_month, arrayOf("septiembre de 2026")), actual.failed[0].title)
            assertEquals(UiText.Resource(Res.string.reason_transient), actual.failed[1].reason)
        }

    @Test
    fun `When a failed task is retried then it is requeued and the status reloads`() =
        runTest(dispatcher) {
            repository.statusResult = Result.Success(failedStatus)
            val sut = sut()
            repository.statusResult = Result.Success(SheetsStatus(Instant.parse("2026-10-18T10:05:00Z"), 2, 1, failedStatus.recent.drop(1)))

            sut.uiEvents.test {
                sut.onAction(GoogleDriveUiAction.RetryTask("f1"))

                assertEquals(GoogleDriveUiEvent.ShowMessage(UiText.Resource(Res.string.status_retried)), awaitItem())
            }
            assertEquals(listOf("f1"), repository.retried)
            assertEquals(
                listOf("f2"),
                sut
                    .loaded()
                    .status!!
                    .failed
                    .map { it.id },
            )
            assertFalse(
                sut
                    .loaded()
                    .status!!
                    .failed
                    .single()
                    .isRetrying,
            )
        }

    @Test
    fun `When all failed tasks are retried then the screen says how many went back to the queue`() =
        runTest(dispatcher) {
            repository.statusResult = Result.Success(failedStatus)
            val sut = sut()

            sut.uiEvents.test {
                sut.onAction(GoogleDriveUiAction.RetryAllFailed)

                assertEquals(GoogleDriveUiEvent.ShowMessage(UiText.Resource(Res.string.status_retried_all, arrayOf(3))), awaitItem())
            }
            assertFalse(sut.loaded().status!!.isRetryingAll)
        }

    @Test
    fun `When the receipts of a month are resynced then the rows rewritten are shown`() =
        runTest(dispatcher) {
            repository.taskAnswers =
                listOf(
                    Result.Success(
                        task(id = "t2", kind = SheetTaskKind.INVOICE_MONTH, reference = "2026-09", rows = 4, pendingLastNames = null),
                    ),
                )
            val sut = sut()

            sut.onAction(GoogleDriveUiAction.SelectResyncMonth(SheetMonth(2026, 9)))
            sut.onAction(GoogleDriveUiAction.Resync)

            assertEquals(listOf(SheetMonth(2026, 9)), repository.resynced)
            assertEquals(
                TaskProgressUiState.Done(UiText.Resource(Res.string.resync_done, arrayOf(4, "septiembre de 2026")), null),
                sut.loaded().resync,
            )
        }
}
