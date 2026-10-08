package com.granatum.feature.invoicing.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

enum class InvoiceState { PENDING_RECOGNITION, DRAFT, CONFIRMED, DISCARDED }

enum class InvoiceType { ISSUED, RECEIVED }

/** Why a VAT line has no quota; required when it is zero. */
enum class NoQuotaCause { EXEMPT, REVERSE_CHARGE, INTRA_EU }

/** The fields the server can attach a warning to (`AvisoDto.campo`). */
object InvoiceField {
    const val TYPE = "tipo"
    const val ISSUER_NAME = "emisor.nombre"
    const val ISSUER_TAX_ID = "emisor.nif"
    const val RECIPIENT_NAME = "destinatario.nombre"
    const val RECIPIENT_TAX_ID = "destinatario.nif"
    const val NUMBER = "numero"
    const val ISSUE_DATE = "fechaEmision"
    const val CONCEPT = "concepto"
    const val CURRENCY = "moneda"
    const val LINES = "lineas"
    const val WITHHOLDING = "retenciones"
    const val TOTAL = "total"
}

data class Party(val name: String?, val taxId: String?) {
    val isEmpty: Boolean get() = name.isNullOrBlank() && taxId.isNullOrBlank()
}

data class VatLine(
    val rate: Money,
    val base: Money,
    val quota: Money,
    val surcharge: Money = Money.ZERO,
    val noQuotaCause: NoQuotaCause? = null
) {
    val amount: Money get() = base + quota + surcharge
}

data class InvoiceWarning(val field: String, val code: String, val blocking: Boolean, val message: String)

data class RecognitionSummary(val result: String, val doubtfulFields: List<String>)

data class Invoice(
    val id: String,
    val state: InvoiceState,
    val type: InvoiceType?,
    val version: Int,
    val issuer: Party?,
    val recipient: Party?,
    val number: String?,
    val issueDate: LocalDate?,
    val concept: String?,
    val currency: String,
    val corrective: Boolean,
    val lines: List<VatLine>,
    val withholding: Money,
    val total: Money?,
    val quarterClosed: Boolean,
    val recognition: RecognitionSummary?,
    val warnings: List<InvoiceWarning>
) {
    val hasBlockingWarnings: Boolean get() = warnings.any { it.blocking }

    /** A confirmed invoice can still be corrected while its quarter is open; a discarded one never. */
    val canEdit: Boolean get() = when (state) {
        InvoiceState.PENDING_RECOGNITION, InvoiceState.DRAFT -> true
        InvoiceState.CONFIRMED -> !quarterClosed
        InvoiceState.DISCARDED -> false
    }
    val canConfirm: Boolean get() = state == InvoiceState.DRAFT
    val canDiscard: Boolean get() = canEdit
    val canRecognize: Boolean get() = state == InvoiceState.PENDING_RECOGNITION || state == InvoiceState.DRAFT

    fun draft(): InvoiceDraft = InvoiceDraft(
        type = type, issuer = issuer, recipient = recipient, number = number, issueDate = issueDate,
        concept = concept, currency = currency, corrective = corrective, lines = lines,
        withholding = withholding, total = total, version = version
    )
}

/** What a save sends: every editable field plus the version it was read at (research D4). */
data class InvoiceDraft(
    val type: InvoiceType?,
    val issuer: Party?,
    val recipient: Party?,
    val number: String?,
    val issueDate: LocalDate?,
    val concept: String?,
    val currency: String = "EUR",
    val corrective: Boolean,
    val lines: List<VatLine>,
    val withholding: Money,
    val total: Money?,
    val version: Int
) {
    /** What the lines add up to once withholding is taken off: what the total should be. */
    val expectedTotal: Money get() = lines.fold(Money.ZERO) { acc, line -> acc + line.amount } - withholding

    /** Total minus what the lines say; the server allows one cent either way. Null without a total. */
    val balanceGap: Money? get() = total?.let { it - expectedTotal }

    val isBalanced: Boolean get() = balanceGap?.let { it.cents in -1L..1L } ?: false
}

data class InvoiceSummary(
    val id: String,
    val state: InvoiceState,
    val type: InvoiceType?,
    val issuer: Party?,
    val recipient: Party?,
    val number: String?,
    val issueDate: LocalDate?,
    val total: Money?,
    val warningCount: Int
) {
    /** The other party: whoever is not the company, as far as the type tells. */
    val counterparty: Party? get() = when (type) {
        InvoiceType.ISSUED -> recipient ?: issuer
        else -> issuer ?: recipient
    }
}

data class Page<T>(val items: List<T>, val page: Int, val size: Int, val total: Long) {
    val hasMore: Boolean get() = (page + 1).toLong() * size < total
}

data class InvoiceFilter(
    val state: InvoiceState? = null,
    val type: InvoiceType? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val text: String = ""
)

class UploadDocument(val bytes: ByteArray, val fileName: String, val mimeType: String)

enum class UploadOutcome { ACCEPTED, DUPLICATE, UNSUPPORTED_FORMAT, TOO_LARGE, EMPTY, UNREADABLE_PDF, UNKNOWN }

/** [position] is 1-based, in the order the files were sent. */
data class UploadResult(val position: Int, val outcome: UploadOutcome, val invoiceId: String?)

data class RecognitionAttempt(val result: String, val model: String, val createdAt: Instant, val error: String?)

enum class ChangeAction { CORRECTION, DISCARD, RECLASSIFICATION, OTHER }

data class InvoiceChange(val action: ChangeAction, val authorId: String, val at: Instant, val previousValues: Map<String, String>)

data class InvoiceHistory(val recognitions: List<RecognitionAttempt>, val changes: List<InvoiceChange>)

/** A downloaded file: the original document or a report. */
class InvoicingFile(val bytes: ByteArray, val fileName: String, val mimeType: String) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}
