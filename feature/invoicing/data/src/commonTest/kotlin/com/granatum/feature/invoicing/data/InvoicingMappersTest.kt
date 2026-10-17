package com.granatum.feature.invoicing.data

import com.granatum.feature.invoicing.data.dto.ChangeHistoryDto
import com.granatum.feature.invoicing.data.dto.ClosedQuarterDto
import com.granatum.feature.invoicing.data.dto.CompanyDto
import com.granatum.feature.invoicing.data.dto.HistoryDto
import com.granatum.feature.invoicing.data.dto.InvoiceDto
import com.granatum.feature.invoicing.data.dto.InvoiceSummaryDto
import com.granatum.feature.invoicing.data.dto.NoticeDto
import com.granatum.feature.invoicing.data.dto.PartyDto
import com.granatum.feature.invoicing.data.dto.QuarterEventDto
import com.granatum.feature.invoicing.data.dto.QuarterStatusDto
import com.granatum.feature.invoicing.data.dto.RecognitionHistoryDto
import com.granatum.feature.invoicing.data.dto.RecognitionSummaryDto
import com.granatum.feature.invoicing.data.dto.ReportDto
import com.granatum.feature.invoicing.data.dto.ReportPeriodDto
import com.granatum.feature.invoicing.data.dto.ReportTotalsDto
import com.granatum.feature.invoicing.data.dto.UploadResultDto
import com.granatum.feature.invoicing.data.dto.VatLineDto
import com.granatum.feature.invoicing.data.mappers.toApi
import com.granatum.feature.invoicing.data.mappers.toDomain
import com.granatum.feature.invoicing.data.mappers.toInvoiceState
import com.granatum.feature.invoicing.data.mappers.toInvoiceType
import com.granatum.feature.invoicing.data.mappers.toNoQuotaCause
import com.granatum.feature.invoicing.domain.model.ChangeAction
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.Money
import com.granatum.feature.invoicing.domain.model.NoQuotaCause
import com.granatum.feature.invoicing.domain.model.Party
import com.granatum.feature.invoicing.domain.model.QuarterAction
import com.granatum.feature.invoicing.domain.model.ReportPeriod
import com.granatum.feature.invoicing.domain.model.UploadOutcome
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class InvoicingMappersTest {

    @Test
    fun invoice_states_round_trip_and_unknown_means_discarded() {
        for (state in InvoiceState.entries) assertEquals(state, state.toApi().toInvoiceState())
        assertEquals(InvoiceState.DISCARDED, "SOMETHING_NEW".toInvoiceState())
    }

    @Test
    fun invoice_types_and_no_quota_causes_round_trip() {
        for (type in InvoiceType.entries) assertEquals(type, type.toApi().toInvoiceType())
        assertNull("OTHER".toInvoiceType())
        assertNull(null.toInvoiceType())
        for (cause in NoQuotaCause.entries) assertEquals(cause, cause.toApi().toNoQuotaCause())
        assertNull("???".toNoQuotaCause())
    }

    @Test
    fun an_invoice_dto_maps_every_field_and_tolerates_bad_input() {
        val dto = InvoiceDto(
            id = "f1", status = "DRAFT", type = "RECEIVED", version = 3,
            issuer = PartyDto("Proveedor", "B1"), recipient = null,
            number = "A-1", issueDate = "2026-03-01", concept = "Harina",
            corrective = true,
            vatLines = listOf(VatLineDto("21.00", "100.00", "21.00", noVatCause = "EXEMPT")),
            withholdings = "5,00", total = "116.00", quarterClosed = true,
            recognition = RecognitionSummaryDto("RECOGNIZED", listOf("number")),
            notices = listOf(NoticeDto("number", "UNCERTAIN", true, "Revisar"))
        )

        val invoice = dto.toDomain()

        assertEquals(InvoiceState.DRAFT, invoice.state)
        assertEquals(InvoiceType.RECEIVED, invoice.type)
        assertEquals(Party("Proveedor", "B1"), invoice.issuer)
        assertEquals(LocalDate(2026, 3, 1), invoice.issueDate)
        assertEquals(Money(500), invoice.withholding)
        assertEquals(Money(11600), invoice.total)
        assertEquals(NoQuotaCause.EXEMPT, invoice.lines.single().noQuotaCause)
        assertEquals(Money(2100), invoice.lines.single().rate)
        assertEquals(listOf("number"), invoice.recognition?.doubtfulFields)
        assertTrue(invoice.warnings.single().blocking)
        assertTrue(invoice.quarterClosed)

        val broken = dto.copy(issueDate = "not-a-date", withholdings = "x", total = "x").toDomain()
        assertNull(broken.issueDate)
        assertEquals(Money.ZERO, broken.withholding)
        assertNull(broken.total)
    }

    @Test
    fun a_summary_reads_the_total_as_text_and_drops_empty_parties() {
        val summary = InvoiceSummaryDto(
            id = "s1", status = "CONFIRMED", type = "ISSUED",
            issuerName = "Yo", issuerNif = "B2", number = "7", issueDate = "2026-01-31",
            total = JsonPrimitive(12.345), noticeCount = 2
        ).toDomain()

        assertEquals(Party("Yo", "B2"), summary.issuer)
        assertNull(summary.recipient)
        assertEquals(Money(1234), summary.total)
        assertEquals(2, summary.warningCount)

        assertNull(InvoiceSummaryDto("s2", "DRAFT", total = JsonNull).toDomain().total)
        assertNull(InvoiceSummaryDto("s3", "DRAFT").toDomain().total)
    }

    @Test
    fun upload_results_map_every_outcome() {
        val expected = mapOf(
            "ACCEPTED" to UploadOutcome.ACCEPTED,
            "DUPLICATE" to UploadOutcome.DUPLICATE,
            "UNSUPPORTED_FORMAT" to UploadOutcome.UNSUPPORTED_FORMAT,
            "TOO_LARGE" to UploadOutcome.TOO_LARGE,
            "EMPTY" to UploadOutcome.EMPTY,
            "UNREADABLE_PDF" to UploadOutcome.UNREADABLE_PDF,
            "NEW" to UploadOutcome.UNKNOWN
        )
        for ((wire, outcome) in expected) {
            val result = UploadResultDto(file = 2, result = wire, invoiceId = "f").toDomain()
            assertEquals(outcome, result.outcome, wire)
            assertEquals(2, result.position)
            assertEquals("f", result.invoiceId)
        }
    }

    @Test
    fun history_is_sorted_newest_first_and_flattens_free_form_values() {
        val history = HistoryDto(
            recognitions = listOf(
                RecognitionHistoryDto("RECOGNIZED", "m1", "2026-01-01T10:00:00Z"),
                RecognitionHistoryDto("API_ERROR", "m2", "2026-02-01T10:00:00Z", error = "boom"),
                RecognitionHistoryDto("RECOGNIZED", "m3", "broken date")
            ),
            changes = listOf(
                ChangeHistoryDto(
                    "CORRECTION", "u1", "2026-01-05T08:00:00Z",
                    JsonObject(mapOf("a" to JsonNull, "b" to JsonPrimitive("x"), "c" to JsonArray(listOf(JsonPrimitive(1)))))
                ),
                ChangeHistoryDto("DISCARD", "u2", "2026-01-06T08:00:00Z"),
                ChangeHistoryDto("RECLASSIFICATION", "u3", "2026-01-04T08:00:00Z"),
                ChangeHistoryDto("OTHER", "u4", "2026-01-03T08:00:00Z")
            )
        ).toDomain()

        assertEquals(listOf("m2", "m1", "m3"), history.recognitions.map { it.model })
        assertEquals("boom", history.recognitions.first().error)
        assertEquals(Instant.fromEpochSeconds(0), history.recognitions.last().createdAt)
        assertEquals(
            listOf(ChangeAction.DISCARD, ChangeAction.CORRECTION, ChangeAction.RECLASSIFICATION, ChangeAction.OTHER),
            history.changes.map { it.action }
        )
        assertEquals(mapOf("a" to "", "b" to "x", "c" to "[1]"), history.changes[1].previousValues)
        assertEquals(emptyMap(), history.changes[0].previousValues)
    }

    @Test
    fun a_quarter_maps_its_events_newest_first() {
        val quarter = QuarterStatusDto(
            quarter = 2, closed = true,
            events = listOf(
                QuarterEventDto("CLOSE", "u1", "2026-04-01T00:00:00Z"),
                QuarterEventDto("REOPEN", "u2", "2026-05-01T00:00:00Z", "Error de alta"),
                QuarterEventDto("OTHER", "u3", "2026-03-01T00:00:00Z")
            )
        ).toDomain(2026)

        assertEquals(2026, quarter.year)
        assertEquals(2, quarter.quarter)
        assertTrue(quarter.closed)
        assertEquals(listOf(QuarterAction.REOPEN, QuarterAction.CLOSE, QuarterAction.OTHER), quarter.events.map { it.action })
        assertEquals("Error de alta", quarter.events.first().reason)
    }

    @Test
    fun a_company_and_a_report_map_their_figures() {
        val company = CompanyDto("Granatum SL", "B123", true).toDomain()
        assertEquals("Granatum SL", company.legalName)
        assertEquals("B123", company.taxId)
        assertTrue(company.recognitionEnabled)

        val report = ReportDto(
            period = ReportPeriodDto("QUARTERLY", 2026, quarter = 1, from = "2026-01-01", to = "2026-03-31"),
            issued = ReportTotalsDto(
                invoices = 2, base = "200.00",
                vatByRate = mapOf("21.00" to "42.00", "10.00" to "5.00"),
                surcharge = "1.00", withholdings = "2.00", total = "248.00",
                noVatBase = mapOf("EXEMPT" to "30.00", "ODD" to "9.00")
            ),
            received = ReportTotalsDto(),
            pending = 4,
            closedQuarters = listOf(ClosedQuarterDto(2025, 4)),
            calculatedAt = "2026-04-02T09:30:00Z"
        ).toDomain(ReportPeriod.Quarterly(2026, 1))

        assertEquals(LocalDate(2026, 1, 1), report.from)
        assertEquals(LocalDate(2026, 3, 31), report.to)
        assertEquals(4, report.pending)
        assertEquals(listOf(2025 to 4), report.closedQuarters)
        assertEquals(2, report.issued.count)
        assertEquals(listOf("10.00", "21.00"), report.issued.vatByRate.keys.toList())
        assertEquals(Money(4200), report.issued.vatByRate["21.00"])
        assertEquals(mapOf(NoQuotaCause.EXEMPT to Money(3000)), report.issued.noQuota)
        assertEquals(Money.ZERO, report.received.total)
        assertEquals(Instant.parse("2026-04-02T09:30:00Z"), report.computedAt)
    }
}
