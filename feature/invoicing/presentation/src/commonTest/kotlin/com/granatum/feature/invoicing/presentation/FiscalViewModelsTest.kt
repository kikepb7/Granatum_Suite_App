package com.granatum.feature.invoicing.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.feature.invoicing.domain.model.InvoicingError
import com.granatum.feature.invoicing.domain.model.ReportFormat
import com.granatum.feature.invoicing.domain.model.ReportPeriod
import com.granatum.feature.invoicing.domain.usecase.InvoicingUseCases
import com.granatum.feature.invoicing.presentation.company.CompanyIssue
import com.granatum.feature.invoicing.presentation.company.CompanyViewModel
import com.granatum.feature.invoicing.presentation.quarters.QuartersAction
import com.granatum.feature.invoicing.presentation.quarters.QuartersViewModel
import com.granatum.feature.invoicing.presentation.report.PeriodType
import com.granatum.feature.invoicing.presentation.report.ReportAction
import com.granatum.feature.invoicing.presentation.report.ReportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FiscalViewModelsTest {
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeInvoicingRepository()

    @Test
    fun quarters_close_with_confirmation_and_reopen_with_a_reason() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = QuartersViewModel(InvoicingUseCases(repo), currentYear = 2026)
            val q2 = vm.state.value.quarters[1]

            vm.onAction(QuartersAction.OnClose(q2))
            assertTrue(repo.closed.isEmpty())
            vm.onAction(QuartersAction.OnCloseConfirm)
            assertEquals(listOf(2026 to 2), repo.closed)
            assertTrue(
                vm.state.value.quarters[1]
                    .closed,
            )

            val q1 = vm.state.value.quarters[0]
            vm.onAction(QuartersAction.OnReopen(q1))
            vm.reason.setTextAndPlaceCursorAtEnd("corto")
            vm.onAction(QuartersAction.OnReopenConfirm)
            assertTrue(vm.state.value.reasonInvalid)
            assertTrue(repo.reopened.isEmpty())
            vm.reason.setTextAndPlaceCursorAtEnd("  Falta una factura de septiembre ")
            vm.onAction(QuartersAction.OnReopenConfirm)
            assertEquals(Triple(2026, 1, "Falta una factura de septiembre"), repo.reopened.single())
        }

    @Test
    fun a_quarter_with_pending_invoices_says_so() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = QuartersViewModel(InvoicingUseCases(repo), currentYear = 2026)
            repo.failNext = InvoicingError.QuarterHasPending("Quedan 3 facturas pendientes")

            vm.onAction(QuartersAction.OnClose(vm.state.value.quarters[2]))
            vm.onAction(QuartersAction.OnCloseConfirm)

            assertNotNull(vm.state.value.message)
            assertTrue(
                !vm.state.value.quarters[2]
                    .closed,
            )
        }

    @Test
    fun the_year_can_change() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = QuartersViewModel(InvoicingUseCases(repo), currentYear = 2026)
            vm.onAction(QuartersAction.OnYear(-1))
            assertEquals(listOf(2026, 2025), repo.quarterYears)
            assertEquals(
                2025,
                vm.state.value.quarters
                    .first()
                    .year,
            )
        }

    @Test
    fun the_report_steps_through_periods_and_downloads() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = ReportViewModel(InvoicingUseCases(repo), year = 2026, month = 1)
            assertEquals(ReportPeriod.Quarterly(2026, 1), repo.reports.last())

            vm.onAction(ReportAction.OnStep(-1))
            assertEquals(ReportPeriod.Quarterly(2025, 4), repo.reports.last())
            vm.onAction(ReportAction.OnType(PeriodType.MONTHLY))
            assertEquals(ReportPeriod.Monthly(2025, 10), repo.reports.last(), "the first month of the quarter shown")
            vm.onAction(ReportAction.OnStep(-1))
            assertEquals(ReportPeriod.Monthly(2025, 9), repo.reports.last())
            vm.onAction(ReportAction.OnType(PeriodType.QUARTERLY))
            assertEquals(ReportPeriod.Quarterly(2025, 3), repo.reports.last())
            vm.onAction(ReportAction.OnType(PeriodType.YEARLY))
            assertEquals(ReportPeriod.Yearly(2025), repo.reports.last())

            val files = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.fileEvents.collect { files += it.fileName } }
            vm.onAction(ReportAction.OnDownload(ReportFormat.PDF))
            assertEquals(listOf("reporte.pdf"), files)
            assertNull(vm.state.value.downloading)
        }

    @Test
    fun the_company_is_validated_and_the_stored_nif_shown() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            repo.company = null
            val vm = CompanyViewModel(InvoicingUseCases(repo))
            assertNull(vm.state.value.recognitionEnabled)

            vm.save()
            assertEquals(setOf(CompanyIssue.NAME_REQUIRED, CompanyIssue.TAX_ID_REQUIRED), vm.state.value.issues)

            vm.legalName.setTextAndPlaceCursorAtEnd("Granatum Flores SL")
            vm.taxId.setTextAndPlaceCursorAtEnd("b 12345678")
            repo.failNext = InvoicingError.InvalidTaxId
            vm.save()
            assertEquals(setOf(CompanyIssue.TAX_ID_INVALID), vm.state.value.issues)

            vm.save()
            assertEquals("B12345678", vm.taxId.text.toString())
            assertEquals(false, vm.state.value.recognitionEnabled)
            assertNotNull(vm.state.value.message)
        }
}
