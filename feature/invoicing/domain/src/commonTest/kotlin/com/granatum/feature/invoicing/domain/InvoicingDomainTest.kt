package com.granatum.feature.invoicing.domain

import com.granatum.feature.invoicing.domain.model.Invoice
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceWarning
import com.granatum.feature.invoicing.domain.model.Money
import com.granatum.feature.invoicing.domain.model.Page
import com.granatum.feature.invoicing.domain.model.VatLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvoicingDomainTest {

    @Test
    fun money_reads_dots_and_commas_and_rejects_the_rest() {
        assertEquals(Money(123456), Money.parse("1234.56"))
        assertEquals(Money(123450), Money.parse("1234,5"))
        assertEquals(Money(-1000), Money.parse("-10"))
        assertEquals(Money(999_999_999_999), Money.parse("9999999999.99"))
        assertNull(Money.parse("1.234,56"))
        assertNull(Money.parse("12345678901"))
        assertNull(Money.parse("1.234"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
    }

    @Test
    fun money_writes_for_the_server_and_for_people() {
        assertEquals("1234.56", Money(123456).toApi())
        assertEquals("-0.05", Money(-5).toApi())
        assertEquals("1.234.567,08", Money(123456708).format())
        assertEquals("0,00", Money.ZERO.format())
        assertEquals("1234,56", Money(123456).toInput())
    }

    @Test
    fun json_numbers_are_read_without_floating_point() {
        assertEquals(Money(1210), Money.parseNumber("12.1"))
        assertEquals(Money(1210), Money.parseNumber("12.10"))
        assertEquals(Money(1200), Money.parseNumber("12"))
        assertNull(Money.parseNumber("1.2E3"))
    }

    @Test
    fun the_balance_is_lines_minus_withholding_with_a_cent_of_tolerance() {
        val draft = invoice(InvoiceState.DRAFT).draft().copy(
            lines = listOf(VatLine(rate = Money(2100), base = Money(10000), quota = Money(2100))),
            withholding = Money(1500),
            total = Money(10601)
        )
        assertEquals(Money(10600), draft.expectedTotal)
        assertEquals(Money(1), draft.balanceGap)
        assertTrue(draft.isBalanced)
        assertFalse(draft.copy(total = Money(10700)).isBalanced)
        assertFalse(draft.copy(total = null).isBalanced)
    }

    @Test
    fun what_can_be_done_depends_on_the_state_and_the_quarter() {
        assertTrue(invoice(InvoiceState.DRAFT).canConfirm)
        assertTrue(invoice(InvoiceState.PENDING_RECOGNITION).canRecognize)
        assertFalse(invoice(InvoiceState.CONFIRMED).canConfirm)
        assertTrue(invoice(InvoiceState.CONFIRMED).canEdit)
        assertFalse(invoice(InvoiceState.CONFIRMED, quarterClosed = true).canEdit)
        assertFalse(invoice(InvoiceState.CONFIRMED, quarterClosed = true).canDiscard)
        assertFalse(invoice(InvoiceState.DISCARDED).canEdit)
        assertFalse(invoice(InvoiceState.DISCARDED).canRecognize)
    }

    @Test
    fun blocking_warnings_and_pages() {
        val withWarning = invoice(InvoiceState.DRAFT).copy(warnings = listOf(InvoiceWarning("total", "NO_CUADRA", true, "")))
        assertTrue(withWarning.hasBlockingWarnings)
        assertTrue(Page(listOf(1), page = 0, size = 50, total = 51).hasMore)
        assertFalse(Page(listOf(1), page = 1, size = 50, total = 100).hasMore)
    }

    private fun invoice(state: InvoiceState, quarterClosed: Boolean = false) = Invoice(
        id = "f1", state = state, type = null, version = 0, issuer = null, recipient = null, number = null,
        issueDate = null, concept = null, currency = "EUR", corrective = false, lines = emptyList(),
        withholding = Money.ZERO, total = null, quarterClosed = quarterClosed, recognition = null, warnings = emptyList()
    )
}
