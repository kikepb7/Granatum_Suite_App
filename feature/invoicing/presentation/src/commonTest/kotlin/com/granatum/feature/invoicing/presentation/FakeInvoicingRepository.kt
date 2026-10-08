package com.granatum.feature.invoicing.presentation

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.invoicing.domain.model.Company
import com.granatum.feature.invoicing.domain.model.Invoice
import com.granatum.feature.invoicing.domain.model.InvoiceDraft
import com.granatum.feature.invoicing.domain.model.InvoiceFilter
import com.granatum.feature.invoicing.domain.model.InvoiceHistory
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceSummary
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.InvoicingError
import com.granatum.feature.invoicing.domain.model.InvoicingFile
import com.granatum.feature.invoicing.domain.model.Money
import com.granatum.feature.invoicing.domain.model.Page
import com.granatum.feature.invoicing.domain.model.Party
import com.granatum.feature.invoicing.domain.model.Quarter
import com.granatum.feature.invoicing.domain.model.Report
import com.granatum.feature.invoicing.domain.model.ReportFormat
import com.granatum.feature.invoicing.domain.model.ReportGroup
import com.granatum.feature.invoicing.domain.model.ReportPeriod
import com.granatum.feature.invoicing.domain.model.UploadDocument
import com.granatum.feature.invoicing.domain.model.UploadOutcome
import com.granatum.feature.invoicing.domain.model.UploadResult
import com.granatum.feature.invoicing.domain.model.VatLine
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

fun summary(
    id: String,
    state: InvoiceState = InvoiceState.CONFIRMED,
) = InvoiceSummary(
    id = id,
    state = state,
    type = InvoiceType.RECEIVED,
    issuer = Party("Flores SL", "B12345678"),
    recipient = null,
    number = "A-$id",
    issueDate = LocalDate(2026, 9, 1),
    total = Money(12100),
    warningCount = 0,
)

fun invoice(
    id: String = "f1",
    state: InvoiceState = InvoiceState.DRAFT,
    version: Int = 1,
    total: Money? = Money(12100),
    quarterClosed: Boolean = false,
    blocking: Boolean = false,
) = Invoice(
    id = id,
    state = state,
    type = InvoiceType.RECEIVED,
    version = version,
    issuer = Party("Flores SL", "B12345678"),
    recipient = null,
    number = "A-1",
    issueDate = LocalDate(2026, 9, 1),
    concept = null,
    currency = "EUR",
    corrective = false,
    lines = listOf(VatLine(rate = Money(2100), base = Money(10000), quota = Money(2100))),
    withholding = Money.ZERO,
    total = total,
    quarterClosed = quarterClosed,
    recognition = null,
    warnings =
        if (blocking) {
            listOf(
                com.granatum.feature.invoicing.domain.model
                    .InvoiceWarning("total", "NO_CUADRA", true, "No cuadra"),
            )
        } else {
            emptyList()
        },
)

private val emptyGroup = ReportGroup(0, Money.ZERO, emptyMap(), Money.ZERO, Money.ZERO, Money.ZERO, emptyMap())

/** In-memory repository: records what the screens ask for; [failNext] makes the next write fail. */
class FakeInvoicingRepository : InvoicingRepositoryRecorder() {
    val pages = mutableMapOf<Int, Page<InvoiceSummary>>()
    var listFailure: InvoicingError? = null
    val invoices = mutableMapOf<String, Invoice>()
    var company: Company? = Company("Granatum Flores SL", "B12345678", recognitionEnabled = true)
    var failNext: InvoicingError? = null
    var uploadOutcome: (UploadDocument) -> UploadOutcome = { UploadOutcome.ACCEPTED }
    var savedWarnings: Boolean = false
    val quarterList = (1..4).map { Quarter(2026, it, closed = it == 1, events = emptyList()) }.toMutableList()

    private fun <T> write(value: () -> T): Result<T, InvoicingError> {
        val failure = failNext
        failNext = null
        return if (failure != null) Result.Failure(failure) else Result.Success(value())
    }

    override suspend fun invoices(
        filter: InvoiceFilter,
        page: Int,
    ): Result<Page<InvoiceSummary>, InvoicingError> {
        listCalls += filter to page
        listFailure?.let { return Result.Failure(it) }
        return Result.Success(pages[page] ?: Page(emptyList(), page, 50, 0))
    }

    override suspend fun invoice(id: String): Result<Invoice, InvoicingError> {
        reads += id
        return invoices[id]?.let { Result.Success(it) } ?: Result.Failure(InvoicingError.NotFound)
    }

    override suspend fun upload(documents: List<UploadDocument>): Result<List<UploadResult>, InvoicingError> {
        uploads += listOf(documents)
        return write {
            documents.mapIndexed { i, d ->
                UploadResult(
                    i + 1,
                    uploadOutcome(d),
                    if (uploadOutcome(d) ==
                        UploadOutcome.ACCEPTED
                    ) {
                        "n$i"
                    } else {
                        null
                    },
                )
            }
        }
    }

    override suspend fun save(
        id: String,
        draft: InvoiceDraft,
    ): Result<Invoice, InvoicingError> {
        saves += id to draft
        return write {
            val current = invoices.getValue(id)
            val saved =
                current.copy(
                    version = current.version + 1,
                    type = draft.type,
                    issuer = draft.issuer,
                    recipient = draft.recipient,
                    number = draft.number,
                    issueDate = draft.issueDate,
                    concept = draft.concept,
                    corrective = draft.corrective,
                    lines = draft.lines,
                    withholding = draft.withholding,
                    total = draft.total,
                    warnings =
                        if (savedWarnings) {
                            listOf(
                                com.granatum.feature.invoicing.domain.model
                                    .InvoiceWarning("total", "NO_CUADRA", true, "No cuadra"),
                            )
                        } else {
                            emptyList()
                        },
                )
            invoices[id] = saved
            saved
        }
    }

    override suspend fun confirm(
        id: String,
        version: Int,
    ): Result<Invoice, InvoicingError> {
        confirms += id to version
        return write { invoices.getValue(id).copy(state = InvoiceState.CONFIRMED, version = version + 1).also { invoices[id] = it } }
    }

    override suspend fun discard(
        id: String,
        version: Int,
    ): Result<Invoice, InvoicingError> {
        discards += id to version
        return write { invoices.getValue(id).copy(state = InvoiceState.DISCARDED, version = version + 1).also { invoices[id] = it } }
    }

    override suspend fun recognize(id: String): EmptyResult<InvoicingError> {
        recognitions += id
        return write { }
    }

    override suspend fun original(id: String): Result<InvoicingFile, InvoicingError> =
        Result.Success(InvoicingFile(byteArrayOf(1), "factura-$id.pdf", "application/pdf"))

    override suspend fun history(id: String): Result<InvoiceHistory, InvoicingError> =
        Result.Success(InvoiceHistory(emptyList(), emptyList()))

    override suspend fun quarters(year: Int): Result<List<Quarter>, InvoicingError> {
        quarterYears += year
        return Result.Success(quarterList.map { it.copy(year = year) })
    }

    override suspend fun closeQuarter(
        year: Int,
        quarter: Int,
    ): Result<Quarter, InvoicingError> =
        write {
            Quarter(year, quarter, closed = true, events = emptyList()).also { closed += year to quarter }
        }

    override suspend fun reopenQuarter(
        year: Int,
        quarter: Int,
        reason: String,
    ): Result<Quarter, InvoicingError> =
        write {
            Quarter(year, quarter, closed = false, events = emptyList()).also { reopened += Triple(year, quarter, reason) }
        }

    override suspend fun report(period: ReportPeriod): Result<Report, InvoicingError> {
        reports += period
        return Result.Success(
            Report(
                period,
                LocalDate(2026, 1, 1),
                LocalDate(2026, 3, 31),
                emptyGroup,
                emptyGroup,
                0,
                emptyList(),
                Instant.fromEpochSeconds(0),
            ),
        )
    }

    override suspend fun reportFile(
        period: ReportPeriod,
        format: ReportFormat,
    ): Result<InvoicingFile, InvoicingError> =
        write {
            InvoicingFile(
                byteArrayOf(1),
                "reporte.${format.name.lowercase()}",
                if (format ==
                    ReportFormat.PDF
                ) {
                    "application/pdf"
                } else {
                    "text/csv"
                },
            )
        }

    override suspend fun company(): Result<Company?, InvoicingError> = Result.Success(company)

    override suspend fun saveCompany(
        legalName: String,
        taxId: String,
    ): Result<Company, InvoicingError> =
        write {
            Company(legalName, taxId.uppercase().replace(" ", ""), recognitionEnabled = false).also { company = it }
        }
}

/** What the fake saw, kept apart so the fake itself reads as behaviour. */
abstract class InvoicingRepositoryRecorder : com.granatum.feature.invoicing.domain.repository.InvoicingRepository {
    val listCalls = mutableListOf<Pair<InvoiceFilter, Int>>()
    val reads = mutableListOf<String>()
    val uploads = mutableListOf<List<UploadDocument>>()
    val saves = mutableListOf<Pair<String, InvoiceDraft>>()
    val confirms = mutableListOf<Pair<String, Int>>()
    val discards = mutableListOf<Pair<String, Int>>()
    val recognitions = mutableListOf<String>()
    val quarterYears = mutableListOf<Int>()
    val closed = mutableListOf<Pair<Int, Int>>()
    val reopened = mutableListOf<Triple<Int, Int, String>>()
    val reports = mutableListOf<ReportPeriod>()
}
