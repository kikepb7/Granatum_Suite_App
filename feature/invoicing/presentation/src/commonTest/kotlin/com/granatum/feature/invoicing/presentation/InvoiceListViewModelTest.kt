package com.granatum.feature.invoicing.presentation

import com.granatum.feature.invoicing.domain.model.FiscalLimits
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.InvoicingError
import com.granatum.feature.invoicing.domain.model.Page
import com.granatum.feature.invoicing.domain.model.UploadDocument
import com.granatum.feature.invoicing.domain.model.UploadOutcome
import com.granatum.feature.invoicing.domain.usecase.InvoicingUseCases
import com.granatum.feature.invoicing.presentation.list.InvoiceListAction
import com.granatum.feature.invoicing.presentation.list.InvoiceListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InvoiceListViewModelTest {
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo =
        FakeInvoicingRepository().apply {
            pages[0] = Page((1..50).map { summary("a$it") }, 0, 50, 60)
            pages[1] = Page((1..10).map { summary("b$it") }, 1, 50, 60)
        }

    @Test
    fun loads_the_first_page_and_more_on_demand() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceListViewModel(InvoicingUseCases(repo))

            assertEquals(50, vm.state.value.items.size)
            assertTrue(vm.state.value.hasMore)
            vm.onAction(InvoiceListAction.OnLoadMore)
            assertEquals(60, vm.state.value.items.size)
            assertFalse(vm.state.value.hasMore)
            assertFalse(vm.state.value.companyMissing)
        }

    @Test
    fun filters_reload_from_the_first_page() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceListViewModel(InvoicingUseCases(repo))

            vm.onAction(InvoiceListAction.OnState(InvoiceState.DRAFT))
            vm.onAction(InvoiceListAction.OnType(InvoiceType.ISSUED))
            val (filter, page) = repo.listCalls.last()
            assertEquals(InvoiceState.DRAFT, filter.state)
            assertEquals(InvoiceType.ISSUED, filter.type)
            assertEquals(0, page)
            assertTrue(vm.state.value.isFiltered)
        }

    @Test
    fun a_failed_first_page_shows_the_reason() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            repo.listFailure = InvoicingError.NoInternet
            val vm = InvoiceListViewModel(InvoicingUseCases(repo))
            assertNotNull(vm.state.value.error)

            repo.listFailure = null
            vm.onAction(InvoiceListAction.OnRefresh)
            assertNull(vm.state.value.error)
            assertEquals(50, vm.state.value.items.size)
        }

    @Test
    fun pending_invoices_are_polled_until_recognised() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            repo.pages[0] = Page(listOf(summary("p1", InvoiceState.PENDING_RECOGNITION), summary("c1")), 0, 50, 2)
            val vm = InvoiceListViewModel(InvoicingUseCases(repo), pollIntervalMillis = 1_000, maxPolls = 5)
            val callsAfterLoad = repo.listCalls.size

            advanceTimeBy(1_001)
            assertEquals(callsAfterLoad + 1, repo.listCalls.size)
            repo.pages[0] = Page(listOf(summary("p1", InvoiceState.DRAFT), summary("c1")), 0, 50, 2)
            advanceTimeBy(1_000)
            assertEquals(
                InvoiceState.DRAFT,
                vm.state.value.items
                    .first()
                    .state,
            )
            advanceTimeBy(10_000)
            assertEquals(callsAfterLoad + 2, repo.listCalls.size, "polling stops once nothing is pending")
        }

    @Test
    fun uploads_report_each_file_and_skip_oversized_ones() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            repo.uploadOutcome = { if (it.fileName == "dup.jpg") UploadOutcome.DUPLICATE else UploadOutcome.ACCEPTED }
            val vm = InvoiceListViewModel(InvoicingUseCases(repo))
            val big = ByteArray((FiscalLimits.MAX_FILE_BYTES + 1).toInt())

            vm.onAction(
                InvoiceListAction.OnUpload(
                    listOf(
                        UploadDocument(byteArrayOf(1), "foto.jpg", "image/jpeg"),
                        UploadDocument(big, "enorme.pdf", "application/pdf"),
                        UploadDocument(byteArrayOf(2), "dup.jpg", "image/jpeg"),
                    ),
                ),
            )

            assertEquals(listOf("foto.jpg", "dup.jpg"), repo.uploads.single().map { it.fileName })
            val results = vm.state.value.uploadResults!!
            assertEquals(listOf("foto.jpg", "enorme.pdf", "dup.jpg"), results.map { it.fileName })
            assertEquals(listOf(UploadOutcome.ACCEPTED, UploadOutcome.TOO_LARGE, UploadOutcome.DUPLICATE), results.map { it.outcome })
            assertEquals(0, vm.state.value.uploadingCount)
            vm.onAction(InvoiceListAction.OnDismissUploadResults)
            assertNull(vm.state.value.uploadResults)
        }

    @Test
    fun a_large_selection_goes_in_several_requests() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceListViewModel(InvoicingUseCases(repo))
            val nineMegabytes = ByteArray(9 * 1024 * 1024)

            vm.onAction(InvoiceListAction.OnUpload((1..7).map { UploadDocument(nineMegabytes, "f$it.pdf", "application/pdf") }))

            assertEquals(listOf(5, 2), repo.uploads.map { it.size })
            assertEquals(
                7,
                vm.state.value.uploadResults!!
                    .size,
            )
        }

    @Test
    fun a_missing_company_is_flagged() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            repo.company = null
            val vm = InvoiceListViewModel(InvoicingUseCases(repo))
            assertTrue(vm.state.value.companyMissing)
        }
}
