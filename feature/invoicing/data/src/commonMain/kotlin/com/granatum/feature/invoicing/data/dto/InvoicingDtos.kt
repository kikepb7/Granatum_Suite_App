package com.granatum.feature.invoicing.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// Field for field with docs/openapi.json (backend 93ef398), schemas of /api/invoicing.
// Amounts are decimal strings except InvoiceSummary.total, a JSON number (research D2, D12).

/** `InvoicePartyDto`. */
@Serializable
data class PartyDto(
    val name: String? = null,
    val nif: String? = null,
)

/** `InvoiceVatLineDto`. */
@Serializable
data class VatLineDto(
    val vatRate: String,
    val base: String,
    val vatAmount: String,
    val surcharge: String = "0.00",
    val noVatCause: String? = null,
)

/** `InvoiceVatLineRequest`. No defaults: the contract requires every field and kotlinx would leave defaults out. */
@Serializable
data class VatLineRequestDto(
    val vatRate: String,
    val base: String,
    val vatAmount: String,
    val surcharge: String,
    val noVatCause: String?,
)

/** `InvoiceNoticeDto`. */
@Serializable
data class NoticeDto(
    val field: String,
    val code: String,
    val blocking: Boolean,
    val message: String,
)

/** `InvoiceRecognitionSummaryDto`. */
@Serializable
data class RecognitionSummaryDto(
    val result: String,
    val uncertainFields: List<String> = emptyList(),
)

/** `InvoiceDto`. */
@Serializable
data class InvoiceDto(
    val id: String,
    val status: String,
    val type: String? = null,
    val version: Int,
    val issuer: PartyDto? = null,
    val recipient: PartyDto? = null,
    val number: String? = null,
    val issueDate: String? = null,
    val concept: String? = null,
    val currency: String = "EUR",
    val corrective: Boolean = false,
    val vatLines: List<VatLineDto> = emptyList(),
    val withholdings: String = "0.00",
    val total: String? = null,
    val quarterClosed: Boolean = false,
    val recognition: RecognitionSummaryDto? = null,
    val notices: List<NoticeDto> = emptyList(),
)

/** `InvoiceRequest`. */
@Serializable
data class InvoiceRequestDto(
    val type: String?,
    val issuer: PartyDto?,
    val recipient: PartyDto?,
    val number: String?,
    val issueDate: String?,
    val concept: String?,
    val currency: String,
    val corrective: Boolean,
    val vatLines: List<VatLineRequestDto>,
    val withholdings: String,
    val total: String?,
    val version: Int,
)

/** `InvoiceVersionRequest`. */
@Serializable
data class VersionRequestDto(
    val version: Int,
)

/** `InvoiceSummary`. */
@Serializable
data class InvoiceSummaryDto(
    val id: String,
    val status: String,
    val type: String? = null,
    val issuerName: String? = null,
    val issuerNif: String? = null,
    val recipientName: String? = null,
    val recipientNif: String? = null,
    val number: String? = null,
    val issueDate: String? = null,
    /** A JSON number on the wire: kept as an element and read as text, never as Double. */
    val total: JsonElement? = null,
    val noticeCount: Int = 0,
)

/** `InvoicePageInvoiceSummary`. */
@Serializable
data class InvoiceSummaryPageDto(
    val items: List<InvoiceSummaryDto>,
    val page: Int,
    val size: Int,
    val total: Long,
)

/** `InvoiceUploadResultDto`. */
@Serializable
data class UploadResultDto(
    val file: Int,
    val result: String,
    val invoiceId: String? = null,
)

/** `RecognitionHistoryEntry`; its free-form `proposal` is not shown, so it is not read. */
@Serializable
data class RecognitionHistoryDto(
    val result: String,
    val model: String,
    val createdAt: String,
    val error: String? = null,
)

/** `InvoiceChangeHistoryEntry`. */
@Serializable
data class ChangeHistoryDto(
    val action: String,
    val authorId: String,
    val occurredAt: String,
    val previousValues: JsonObject? = null,
)

/** `InvoiceHistory`. */
@Serializable
data class HistoryDto(
    val recognitions: List<RecognitionHistoryDto> = emptyList(),
    val changes: List<ChangeHistoryDto> = emptyList(),
)

/** `QuarterEvent`. */
@Serializable
data class QuarterEventDto(
    val action: String,
    val authorId: String,
    val occurredAt: String,
    val reason: String? = null,
)

/** `QuarterStatus`. */
@Serializable
data class QuarterStatusDto(
    val quarter: Int,
    val closed: Boolean,
    val events: List<QuarterEventDto> = emptyList(),
)

/** `QuarterReopenRequest`. */
@Serializable
data class ReopenRequestDto(
    val reason: String,
)

/** `CompanyDto`. */
@Serializable
data class CompanyDto(
    val legalName: String,
    val nif: String,
    val recognitionActive: Boolean = false,
)

/** `CompanyRequest`. */
@Serializable
data class CompanyRequestDto(
    val legalName: String,
    val nif: String,
)

// GET /reports?format=json: the contract only says "object"; shape from the backend's
// InvoicingReportDtos.kt at 93ef398.

@Serializable
data class ReportPeriodDto(
    val type: String,
    val year: Int,
    val month: Int? = null,
    val quarter: Int? = null,
    val from: String,
    val to: String,
)

@Serializable
data class ReportTotalsDto(
    val invoices: Int = 0,
    val base: String = "0.00",
    val vatByRate: Map<String, String> = emptyMap(),
    val surcharge: String = "0.00",
    val withholdings: String = "0.00",
    val total: String = "0.00",
    val noVatBase: Map<String, String> = emptyMap(),
)

@Serializable
data class ClosedQuarterDto(
    val year: Int,
    val quarter: Int,
    val from: String? = null,
)

@Serializable
data class ReportDto(
    val period: ReportPeriodDto,
    val issued: ReportTotalsDto,
    val received: ReportTotalsDto,
    val pending: Int = 0,
    val closedQuarters: List<ClosedQuarterDto> = emptyList(),
    val calculatedAt: String,
)
