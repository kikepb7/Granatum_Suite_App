package com.granatum.feature.invoicing.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.feature.invoicing.domain.model.InvoiceField
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoicingError
import com.granatum.feature.invoicing.domain.model.Money
import com.granatum.feature.invoicing.domain.model.NoQuotaCause
import com.granatum.feature.invoicing.domain.usecase.InvoicingUseCases
import com.granatum.feature.invoicing.presentation.detail.FormField
import com.granatum.feature.invoicing.presentation.detail.InvoiceDetailAction
import com.granatum.feature.invoicing.presentation.detail.InvoiceDetailViewModel
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InvoiceDetailViewModelTest {
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeInvoicingRepository().apply { invoices["f1"] = invoice() }

    @Test
    fun the_form_starts_with_the_invoice_and_is_clean() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo))

            assertEquals(
                "Flores SL",
                vm.form.issuerName.text
                    .toString(),
            )
            assertEquals(
                "121,00",
                vm.form.total.text
                    .toString(),
            )
            assertEquals(
                "21",
                vm
                    .line(
                        vm.state.value.lineKeys
                            .single(),
                    ).rate.text
                    .toString(),
            )
            assertFalse(vm.isDirty)
            assertTrue(vm.state.value.recognitionEnabled)
        }

    @Test
    fun saving_sends_the_edited_draft_with_its_version() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo))

            vm.form.number.setTextAndPlaceCursorAtEnd("B-7")
            vm.form.withholding.setTextAndPlaceCursorAtEnd("15")
            vm.form.total.setTextAndPlaceCursorAtEnd("106,00")
            assertTrue(vm.isDirty)
            vm.onAction(InvoiceDetailAction.OnSave)

            val (_, draft) = repo.saves.single()
            assertEquals("B-7", draft.number)
            assertEquals(Money(1500), draft.withholding)
            assertEquals(Money(10600), draft.total)
            assertEquals(1, draft.version)
            assertEquals(
                2,
                vm.state.value.invoice
                    ?.version,
            )
            assertFalse(vm.isDirty)
        }

    @Test
    fun bad_amounts_are_flagged_and_not_sent() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo))
            val key =
                vm.state.value.lineKeys
                    .single()

            vm.form.total.setTextAndPlaceCursorAtEnd("12.345,6")
            vm.line(key).rate.setTextAndPlaceCursorAtEnd("150")
            vm.onAction(InvoiceDetailAction.OnSave)

            assertTrue(repo.saves.isEmpty())
            assertTrue(InvoiceField.TOTAL in vm.state.value.badFields)
            assertTrue(FormField.line(0, FormField.RATE) in vm.state.value.badFields)
        }

    @Test
    fun lines_can_be_added_removed_and_given_a_cause() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo))

            vm.onAction(InvoiceDetailAction.OnAddLine)
            val added =
                vm.state.value.lineKeys
                    .last()
            vm.line(added).rate.setTextAndPlaceCursorAtEnd("0")
            vm.line(added).base.setTextAndPlaceCursorAtEnd("5")
            vm.line(added).quota.setTextAndPlaceCursorAtEnd("0")
            vm.onAction(InvoiceDetailAction.OnCause(added, NoQuotaCause.EXEMPT))
            vm.onAction(InvoiceDetailAction.OnUseTotal(vm.currentDraft()!!.expectedTotal))
            vm.onAction(InvoiceDetailAction.OnSave)

            val lines =
                repo.saves
                    .single()
                    .second.lines
            assertEquals(2, lines.size)
            assertEquals(NoQuotaCause.EXEMPT, lines[1].noQuotaCause)
            assertEquals(
                Money(12600),
                repo.saves
                    .single()
                    .second.total,
            )

            vm.onAction(
                InvoiceDetailAction.OnRemoveLine(
                    vm.state.value.lineKeys
                        .last(),
                ),
            )
            assertEquals(1, vm.state.value.lineKeys.size)
        }

    @Test
    fun confirming_saves_pending_edits_first() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo))

            vm.form.concept.setTextAndPlaceCursorAtEnd("Flores de boda")
            vm.onAction(InvoiceDetailAction.OnConfirm)

            assertEquals(1, repo.saves.size)
            assertEquals("f1" to 2, repo.confirms.single(), "confirms the version the save returned")
            assertEquals(
                InvoiceState.CONFIRMED,
                vm.state.value.invoice
                    ?.state,
            )
        }

    @Test
    fun blocking_warnings_stop_the_confirmation() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            repo.invoices["f1"] = invoice(blocking = true)
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo))

            vm.onAction(InvoiceDetailAction.OnConfirm)

            assertTrue(repo.confirms.isEmpty())
            assertNotNull(vm.state.value.message)
        }

    @Test
    fun a_stale_version_reloads_the_invoice() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo))
            vm.form.number.setTextAndPlaceCursorAtEnd("mío")
            repo.invoices["f1"] = invoice(version = 5).copy(number = "de otra persona")
            repo.failNext = InvoicingError.StaleVersion

            vm.onAction(InvoiceDetailAction.OnSave)

            assertEquals(
                5,
                vm.state.value.invoice
                    ?.version,
            )
            assertEquals(
                "de otra persona",
                vm.form.number.text
                    .toString(),
            )
            assertNotNull(vm.state.value.message)
        }

    @Test
    fun a_coherence_error_keeps_what_was_typed() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            repo.invoices["f1"] = invoice(state = InvoiceState.CONFIRMED)
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo))
            vm.form.total.setTextAndPlaceCursorAtEnd("1")
            repo.failNext = InvoicingError.Incoherent("total: NO_CUADRA")

            vm.onAction(InvoiceDetailAction.OnSave)

            assertEquals(
                "1",
                vm.form.total.text
                    .toString(),
            )
        }

    @Test
    fun discarding_asks_first() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo))

            vm.onAction(InvoiceDetailAction.OnDiscard)
            assertTrue(vm.state.value.confirmingDiscard)
            assertTrue(repo.discards.isEmpty())
            vm.onAction(InvoiceDetailAction.OnDiscardConfirm)

            assertEquals("f1" to 1, repo.discards.single())
            assertEquals(
                InvoiceState.DISCARDED,
                vm.state.value.invoice
                    ?.state,
            )
            assertFalse(vm.state.value.canEdit)
        }

    @Test
    fun a_closed_quarter_makes_a_confirmed_invoice_read_only() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            repo.invoices["f1"] = invoice(state = InvoiceState.CONFIRMED, quarterClosed = true)
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo))
            assertFalse(vm.state.value.canEdit)
        }

    @Test
    fun recognising_again_polls_until_the_version_moves() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val vm = InvoiceDetailViewModel("f1", InvoicingUseCases(repo), pollIntervalMillis = 1_000, maxPolls = 5)

            vm.onAction(InvoiceDetailAction.OnRecognize)
            vm.onAction(InvoiceDetailAction.OnRecognizeConfirm)
            assertEquals(listOf("f1"), repo.recognitions)

            repo.invoices["f1"] = invoice(version = 2).copy(number = "Leído")
            advanceTimeBy(1_001)
            assertEquals(
                "Leído",
                vm.form.number.text
                    .toString(),
            )
            val reads = repo.reads.size
            advanceTimeBy(10_000)
            assertEquals(reads, repo.reads.size, "no more polling once done")
        }
}
