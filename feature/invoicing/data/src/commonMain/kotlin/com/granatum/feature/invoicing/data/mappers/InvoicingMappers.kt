package com.granatum.feature.invoicing.data.mappers

import com.granatum.feature.invoicing.data.dto.ChangeHistoryDto
import com.granatum.feature.invoicing.data.dto.CompanyDto
import com.granatum.feature.invoicing.data.dto.HistoryDto
import com.granatum.feature.invoicing.data.dto.InvoiceDto
import com.granatum.feature.invoicing.data.dto.InvoiceRequestDto
import com.granatum.feature.invoicing.data.dto.InvoiceSummaryDto
import com.granatum.feature.invoicing.data.dto.NoticeDto
import com.granatum.feature.invoicing.data.dto.PartyDto
import com.granatum.feature.invoicing.data.dto.QuarterStatusDto
import com.granatum.feature.invoicing.data.dto.ReportDto
import com.granatum.feature.invoicing.data.dto.ReportTotalsDto
import com.granatum.feature.invoicing.data.dto.UploadResultDto
import com.granatum.feature.invoicing.data.dto.VatLineDto
import com.granatum.feature.invoicing.data.dto.VatLineRequestDto
import com.granatum.feature.invoicing.domain.model.ChangeAction
import com.granatum.feature.invoicing.domain.model.Company
import com.granatum.feature.invoicing.domain.model.Invoice
import com.granatum.feature.invoicing.domain.model.InvoiceChange
import com.granatum.feature.invoicing.domain.model.InvoiceDraft
import com.granatum.feature.invoicing.domain.model.InvoiceHistory
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceSummary
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.InvoiceWarning
import com.granatum.feature.invoicing.domain.model.Money
import com.granatum.feature.invoicing.domain.model.NoQuotaCause
import com.granatum.feature.invoicing.domain.model.Party
import com.granatum.feature.invoicing.domain.model.Quarter
import com.granatum.feature.invoicing.domain.model.QuarterAction
import com.granatum.feature.invoicing.domain.model.QuarterEvent
import com.granatum.feature.invoicing.domain.model.RecognitionAttempt
import com.granatum.feature.invoicing.domain.model.RecognitionSummary
import com.granatum.feature.invoicing.domain.model.Report
import com.granatum.feature.invoicing.domain.model.ReportGroup
import com.granatum.feature.invoicing.domain.model.ReportPeriod
import com.granatum.feature.invoicing.domain.model.UploadOutcome
import com.granatum.feature.invoicing.domain.model.UploadResult
import com.granatum.feature.invoicing.domain.model.VatLine
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Instant

fun String.toInvoiceState(): InvoiceState =
    when (this) {
        "PENDING_RECOGNITION" -> InvoiceState.PENDING_RECOGNITION
        "DRAFT" -> InvoiceState.DRAFT
        "CONFIRMED" -> InvoiceState.CONFIRMED
        else -> InvoiceState.DISCARDED
    }

/** The wire values are the enum's own names. */
fun InvoiceState.toApi(): String = name

fun String?.toInvoiceType(): InvoiceType? =
    when (this) {
        "ISSUED" -> InvoiceType.ISSUED
        "RECEIVED" -> InvoiceType.RECEIVED
        else -> null
    }

fun InvoiceType.toApi(): String = name

fun String?.toNoQuotaCause(): NoQuotaCause? =
    when (this) {
        "EXEMPT" -> NoQuotaCause.EXEMPT
        "REVERSE_CHARGE" -> NoQuotaCause.REVERSE_CHARGE
        "INTRA_COMMUNITY" -> NoQuotaCause.INTRA_COMMUNITY
        else -> null
    }

fun NoQuotaCause.toApi(): String = name

private fun money(text: String?): Money = Money.parse(text) ?: Money.ZERO

private fun PartyDto.toDomain() = Party(name = name, taxId = nif)

/** An empty party is sent as null: the server treats both the same and null keeps it simple. */
private fun Party?.toDto(): PartyDto? =
    this?.takeUnless { it.isEmpty }?.let {
        PartyDto(name = it.name?.trim()?.ifEmpty { null }, nif = it.taxId?.trim()?.ifEmpty { null })
    }

fun InvoiceDto.toDomain() =
    Invoice(
        id = id,
        state = status.toInvoiceState(),
        type = type.toInvoiceType(),
        version = version,
        issuer = issuer?.toDomain(),
        recipient = recipient?.toDomain(),
        number = number,
        issueDate = issueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        concept = concept,
        currency = currency,
        corrective = corrective,
        lines = vatLines.map { it.toDomain() },
        withholding = money(withholdings),
        total = Money.parse(total),
        quarterClosed = quarterClosed,
        recognition = recognition?.let { RecognitionSummary(it.result, it.uncertainFields) },
        warnings = notices.map { it.toDomain() },
    )

private fun VatLineDto.toDomain() =
    VatLine(
        rate = money(vatRate),
        base = money(base),
        quota = money(vatAmount),
        surcharge = money(surcharge),
        noQuotaCause = noVatCause.toNoQuotaCause(),
    )

private fun NoticeDto.toDomain() = InvoiceWarning(field = field, code = code, blocking = blocking, message = message)

fun InvoiceDraft.toDto() =
    InvoiceRequestDto(
        type = type?.toApi(),
        issuer = issuer.toDto(),
        recipient = recipient.toDto(),
        number = number?.trim()?.ifEmpty { null },
        issueDate = issueDate?.toString(),
        concept = concept?.trim()?.ifEmpty { null },
        currency = currency,
        corrective = corrective,
        vatLines =
            lines.map {
                VatLineRequestDto(
                    vatRate = it.rate.toApi(),
                    base = it.base.toApi(),
                    vatAmount = it.quota.toApi(),
                    surcharge = it.surcharge.toApi(),
                    noVatCause = it.noQuotaCause?.toApi(),
                )
            },
        withholdings = withholding.toApi(),
        total = total?.toApi(),
        version = version,
    )

fun InvoiceSummaryDto.toDomain() =
    InvoiceSummary(
        id = id,
        state = status.toInvoiceState(),
        type = type.toInvoiceType(),
        issuer = Party(issuerName, issuerNif).takeUnless { it.isEmpty },
        recipient = Party(recipientName, recipientNif).takeUnless { it.isEmpty },
        number = number,
        issueDate = issueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        total = (total as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content?.let { Money.parseNumber(it) },
        warningCount = noticeCount,
    )

fun UploadResultDto.toDomain() =
    UploadResult(
        position = file,
        outcome =
            when (result) {
                "ACCEPTED" -> UploadOutcome.ACCEPTED
                "DUPLICATE" -> UploadOutcome.DUPLICATE
                "UNSUPPORTED_FORMAT" -> UploadOutcome.UNSUPPORTED_FORMAT
                "TOO_LARGE" -> UploadOutcome.TOO_LARGE
                "EMPTY" -> UploadOutcome.EMPTY
                "UNREADABLE_PDF" -> UploadOutcome.UNREADABLE_PDF
                else -> UploadOutcome.UNKNOWN
            },
        invoiceId = invoiceId,
    )

private fun instant(text: String): Instant = runCatching { Instant.parse(text) }.getOrElse { Instant.fromEpochSeconds(0) }

fun HistoryDto.toDomain() =
    InvoiceHistory(
        recognitions =
            recognitions
                .map { RecognitionAttempt(it.result, it.model, instant(it.createdAt), it.error) }
                .sortedByDescending { it.createdAt },
        changes = changes.map { it.toDomain() }.sortedByDescending { it.at },
    )

private fun ChangeHistoryDto.toDomain() =
    InvoiceChange(
        action =
            when (action) {
                "CORRECTION" -> ChangeAction.CORRECTION
                "DISCARD" -> ChangeAction.DISCARD
                "RECLASSIFICATION" -> ChangeAction.RECLASSIFICATION
                else -> ChangeAction.OTHER
            },
        authorId = authorId,
        at = instant(occurredAt),
        // A free-form map (research D12): nested values are shown as their JSON text.
        previousValues =
            previousValues.orEmpty().mapValues { (_, value) ->
                when (value) {
                    is JsonNull -> ""
                    is JsonPrimitive -> value.content
                    else -> value.toString()
                }
            },
    )

fun QuarterStatusDto.toDomain(year: Int) =
    Quarter(
        year = year,
        quarter = quarter,
        closed = closed,
        events =
            events
                .map {
                    QuarterEvent(
                        action =
                            when (it.action) {
                                "CLOSE" -> QuarterAction.CLOSE
                                "REOPEN" -> QuarterAction.REOPEN
                                else -> QuarterAction.OTHER
                            },
                        authorId = it.authorId,
                        at = instant(it.occurredAt),
                        reason = it.reason,
                    )
                }.sortedByDescending { it.at },
    )

fun CompanyDto.toDomain() = Company(legalName = legalName, taxId = nif, recognitionEnabled = recognitionActive)

private fun ReportTotalsDto.toDomain() =
    ReportGroup(
        count = invoices,
        base = money(base),
        vatByRate = vatByRate.entries.sortedBy { money(it.key) }.associate { it.key to money(it.value) },
        surcharge = money(surcharge),
        withholding = money(withholdings),
        total = money(total),
        noQuota = noVatBase.mapNotNull { (cause, base) -> cause.toNoQuotaCause()?.let { it to money(base) } }.toMap(),
    )

fun ReportDto.toDomain(period: ReportPeriod) =
    Report(
        period = period,
        from = LocalDate.parse(this.period.from),
        to = LocalDate.parse(this.period.to),
        issued = issued.toDomain(),
        received = received.toDomain(),
        pending = pending,
        closedQuarters = closedQuarters.map { it.year to it.quarter },
        computedAt = instant(calculatedAt),
    )
