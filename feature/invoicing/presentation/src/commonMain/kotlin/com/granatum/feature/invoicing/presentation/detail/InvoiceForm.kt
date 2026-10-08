package com.granatum.feature.invoicing.presentation.detail

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.feature.invoicing.domain.model.FiscalLimits
import com.granatum.feature.invoicing.domain.model.Invoice
import com.granatum.feature.invoicing.domain.model.InvoiceDraft
import com.granatum.feature.invoicing.domain.model.InvoiceField
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.Money
import com.granatum.feature.invoicing.domain.model.NoQuotaCause
import com.granatum.feature.invoicing.domain.model.Party
import com.granatum.feature.invoicing.domain.model.VatLine
import kotlinx.datetime.LocalDate

/** One VAT line being edited. [key] identifies it in lists; fields read as Compose state. */
class LineForm(
    val key: Long,
) {
    val rate = TextFieldState()
    val base = TextFieldState()
    val quota = TextFieldState()
    val surcharge = TextFieldState("0")

    fun fill(line: VatLine) {
        rate.setTextAndPlaceCursorAtEnd(line.rate.toInput().removeSuffix(",00"))
        base.setTextAndPlaceCursorAtEnd(line.base.toInput())
        quota.setTextAndPlaceCursorAtEnd(line.quota.toInput())
        surcharge.setTextAndPlaceCursorAtEnd(line.surcharge.toInput())
    }
}

/** Field keys for local format problems: the server's names, lines as `lineas[i].campo`. */
object FormField {
    fun line(
        index: Int,
        field: String,
    ) = "${InvoiceField.LINES}[$index].$field"

    const val RATE = "tipoIva"
    const val BASE = "base"
    const val QUOTA = "cuota"
    const val SURCHARGE = "recargo"
    const val CAUSE = "causaSinCuota"
}

/**
 * The editable fields of an invoice. Only the format is checked here (lengths, amounts, rates);
 * business warnings are the server's (research D3).
 */
class InvoiceForm {
    val issuerName = TextFieldState()
    val issuerTaxId = TextFieldState()
    val recipientName = TextFieldState()
    val recipientTaxId = TextFieldState()
    val number = TextFieldState()
    val concept = TextFieldState()
    val withholding = TextFieldState("0")
    val total = TextFieldState()

    fun fill(
        invoice: Invoice,
        lines: List<LineForm>,
    ) {
        issuerName.setTextAndPlaceCursorAtEnd(invoice.issuer?.name.orEmpty())
        issuerTaxId.setTextAndPlaceCursorAtEnd(invoice.issuer?.taxId.orEmpty())
        recipientName.setTextAndPlaceCursorAtEnd(invoice.recipient?.name.orEmpty())
        recipientTaxId.setTextAndPlaceCursorAtEnd(invoice.recipient?.taxId.orEmpty())
        number.setTextAndPlaceCursorAtEnd(invoice.number.orEmpty())
        concept.setTextAndPlaceCursorAtEnd(invoice.concept.orEmpty())
        withholding.setTextAndPlaceCursorAtEnd(invoice.withholding.toInput())
        total.setTextAndPlaceCursorAtEnd(invoice.total?.toInput().orEmpty())
        lines.zip(invoice.lines).forEach { (form, line) -> form.fill(line) }
    }

    sealed interface Parsed {
        data class Valid(
            val draft: InvoiceDraft,
        ) : Parsed

        data class Invalid(
            val badFields: Set<String>,
        ) : Parsed
    }

    fun parse(
        type: InvoiceType?,
        issueDate: LocalDate?,
        corrective: Boolean,
        lines: List<LineForm>,
        causes: Map<Long, NoQuotaCause?>,
        currency: String,
        version: Int,
    ): Parsed {
        val bad = mutableSetOf<String>()

        fun text(
            state: TextFieldState,
            field: String,
            max: Int,
        ): String? {
            val value = state.text.toString().trim()
            if (value.length > max) bad += field
            return value.ifEmpty { null }
        }

        fun amount(
            state: TextFieldState,
            field: String,
            optional: Boolean = false,
        ): Money? {
            val raw = state.text.toString().trim()
            if (raw.isEmpty()) {
                if (!optional) bad += field
                return null
            }
            return Money.parse(raw) ?: run {
                bad += field
                null
            }
        }

        val issuer =
            Party(
                text(issuerName, InvoiceField.ISSUER_NAME, FiscalLimits.NAME),
                text(issuerTaxId, InvoiceField.ISSUER_TAX_ID, FiscalLimits.TAX_ID),
            )
        val recipient =
            Party(
                text(recipientName, InvoiceField.RECIPIENT_NAME, FiscalLimits.NAME),
                text(recipientTaxId, InvoiceField.RECIPIENT_TAX_ID, FiscalLimits.TAX_ID),
            )
        val numberValue = text(number, InvoiceField.NUMBER, FiscalLimits.NUMBER)
        val conceptValue = text(concept, InvoiceField.CONCEPT, FiscalLimits.CONCEPT)
        val withholdingValue = amount(withholding, InvoiceField.WITHHOLDING, optional = true) ?: Money.ZERO
        val totalValue = amount(total, InvoiceField.TOTAL, optional = true)
        val vatLines =
            lines.mapIndexedNotNull { index, line ->
                val rate = amount(line.rate, FormField.line(index, FormField.RATE))
                if (rate != null && (rate.isNegative || rate > FiscalLimits.MAX_RATE)) bad += FormField.line(index, FormField.RATE)
                val base = amount(line.base, FormField.line(index, FormField.BASE))
                val quota = amount(line.quota, FormField.line(index, FormField.QUOTA))
                val surcharge = amount(line.surcharge, FormField.line(index, FormField.SURCHARGE), optional = true) ?: Money.ZERO
                if (rate == null || base == null || quota == null) {
                    null
                } else {
                    VatLine(rate, base, quota, surcharge, causes[line.key]?.takeIf { quota.isZero })
                }
            }
        if (bad.isNotEmpty()) return Parsed.Invalid(bad)
        return Parsed.Valid(
            InvoiceDraft(
                type = type,
                issuer = issuer.takeUnless { it.isEmpty },
                recipient = recipient.takeUnless { it.isEmpty },
                number = numberValue,
                issueDate = issueDate,
                concept = conceptValue,
                currency = currency,
                corrective = corrective,
                lines = vatLines,
                withholding = withholdingValue,
                total = totalValue,
                version = version,
            ),
        )
    }
}
