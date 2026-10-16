package com.granatum.feature.invoicing.data

import com.granatum.feature.invoicing.data.dto.AvisoDto
import com.granatum.feature.invoicing.data.dto.CambioHistorialDto
import com.granatum.feature.invoicing.data.dto.EmpresaDto
import com.granatum.feature.invoicing.data.dto.EstadoTrimestreDto
import com.granatum.feature.invoicing.data.dto.EventoTrimestreDto
import com.granatum.feature.invoicing.data.dto.FacturaDto
import com.granatum.feature.invoicing.data.dto.GrupoReporteDto
import com.granatum.feature.invoicing.data.dto.HistorialDto
import com.granatum.feature.invoicing.data.dto.LineaIvaDto
import com.granatum.feature.invoicing.data.dto.ParteDto
import com.granatum.feature.invoicing.data.dto.PeriodoReporteDto
import com.granatum.feature.invoicing.data.dto.ReconocimientoHistorialDto
import com.granatum.feature.invoicing.data.dto.ReconocimientoResumenDto
import com.granatum.feature.invoicing.data.dto.ReporteDto
import com.granatum.feature.invoicing.data.dto.ResultadoSubidaDto
import com.granatum.feature.invoicing.data.dto.ResumenFacturaDto
import com.granatum.feature.invoicing.data.dto.TrimestreCerradoDto
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
        assertEquals(InvoiceState.DISCARDED, "ALGO_NUEVO".toInvoiceState())
    }

    @Test
    fun invoice_types_and_no_quota_causes_round_trip() {
        for (type in InvoiceType.entries) assertEquals(type, type.toApi().toInvoiceType())
        assertNull("OTRA".toInvoiceType())
        assertNull(null.toInvoiceType())
        for (cause in NoQuotaCause.entries) assertEquals(cause, cause.toApi().toNoQuotaCause())
        assertNull("???".toNoQuotaCause())
    }

    @Test
    fun an_invoice_dto_maps_every_field_and_tolerates_bad_input() {
        val dto = FacturaDto(
            id = "f1", estado = "BORRADOR", tipo = "RECIBIDA", version = 3,
            emisor = ParteDto("Proveedor", "B1"), destinatario = null,
            numero = "A-1", fechaEmision = "2026-03-01", concepto = "Harina",
            rectificativa = true,
            lineas = listOf(LineaIvaDto("21.00", "100.00", "21.00", causaSinCuota = "EXENTA")),
            retenciones = "5,00", total = "116.00", trimestreCerrado = true,
            reconocimiento = ReconocimientoResumenDto("OK", listOf("numero")),
            avisos = listOf(AvisoDto("numero", "DUDOSO", true, "Revisar"))
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
        assertEquals(listOf("numero"), invoice.recognition?.doubtfulFields)
        assertTrue(invoice.warnings.single().blocking)
        assertTrue(invoice.quarterClosed)

        val broken = dto.copy(fechaEmision = "no-es-fecha", retenciones = "x", total = "x").toDomain()
        assertNull(broken.issueDate)
        assertEquals(Money.ZERO, broken.withholding)
        assertNull(broken.total)
    }

    @Test
    fun a_summary_reads_the_total_as_text_and_drops_empty_parties() {
        val summary = ResumenFacturaDto(
            id = "s1", estado = "CONFIRMADA", tipo = "EMITIDA",
            emisorNombre = "Yo", emisorNif = "B2", numero = "7", fechaEmision = "2026-01-31",
            total = JsonPrimitive(12.345), numeroAvisos = 2
        ).toDomain()

        assertEquals(Party("Yo", "B2"), summary.issuer)
        assertNull(summary.recipient)
        assertEquals(Money(1234), summary.total)
        assertEquals(2, summary.warningCount)

        assertNull(ResumenFacturaDto("s2", "BORRADOR", total = JsonNull).toDomain().total)
        assertNull(ResumenFacturaDto("s3", "BORRADOR").toDomain().total)
    }

    @Test
    fun upload_results_map_every_outcome() {
        val expected = mapOf(
            "ACEPTADA" to UploadOutcome.ACCEPTED,
            "DUPLICADA" to UploadOutcome.DUPLICATE,
            "FORMATO_NO_ADMITIDO" to UploadOutcome.UNSUPPORTED_FORMAT,
            "DEMASIADO_GRANDE" to UploadOutcome.TOO_LARGE,
            "VACIO" to UploadOutcome.EMPTY,
            "PDF_NO_LEGIBLE" to UploadOutcome.UNREADABLE_PDF,
            "NUEVO" to UploadOutcome.UNKNOWN
        )
        for ((wire, outcome) in expected) {
            val result = ResultadoSubidaDto(fichero = 2, resultado = wire, facturaId = "f").toDomain()
            assertEquals(outcome, result.outcome, wire)
            assertEquals(2, result.position)
            assertEquals("f", result.invoiceId)
        }
    }

    @Test
    fun history_is_sorted_newest_first_and_flattens_free_form_values() {
        val history = HistorialDto(
            reconocimientos = listOf(
                ReconocimientoHistorialDto("OK", "m1", "2026-01-01T10:00:00Z"),
                ReconocimientoHistorialDto("ERROR", "m2", "2026-02-01T10:00:00Z", error = "boom"),
                ReconocimientoHistorialDto("OK", "m3", "fecha rota")
            ),
            cambios = listOf(
                CambioHistorialDto(
                    "CORRECCION", "u1", "2026-01-05T08:00:00Z",
                    JsonObject(mapOf("a" to JsonNull, "b" to JsonPrimitive("x"), "c" to JsonArray(listOf(JsonPrimitive(1)))))
                ),
                CambioHistorialDto("DESCARTE", "u2", "2026-01-06T08:00:00Z"),
                CambioHistorialDto("RECLASIFICACION", "u3", "2026-01-04T08:00:00Z"),
                CambioHistorialDto("OTRA", "u4", "2026-01-03T08:00:00Z")
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
        val quarter = EstadoTrimestreDto(
            trimestre = 2, cerrado = true,
            eventos = listOf(
                EventoTrimestreDto("CIERRE", "u1", "2026-04-01T00:00:00Z"),
                EventoTrimestreDto("REAPERTURA", "u2", "2026-05-01T00:00:00Z", "Error de alta"),
                EventoTrimestreDto("OTRO", "u3", "2026-03-01T00:00:00Z")
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
        val company = EmpresaDto("Granatum SL", "B123", true).toDomain()
        assertEquals("Granatum SL", company.legalName)
        assertEquals("B123", company.taxId)
        assertTrue(company.recognitionEnabled)

        val report = ReporteDto(
            periodo = PeriodoReporteDto("TRIMESTRAL", 2026, trimestre = 1, desde = "2026-01-01", hasta = "2026-03-31"),
            emitidas = GrupoReporteDto(
                facturas = 2, base = "200.00",
                ivaPorTipo = mapOf("21.00" to "42.00", "10.00" to "5.00"),
                recargo = "1.00", retenciones = "2.00", total = "248.00",
                sinCuota = mapOf("EXENTA" to "30.00", "RARA" to "9.00")
            ),
            recibidas = GrupoReporteDto(),
            pendientes = 4,
            trimestresCerrados = listOf(TrimestreCerradoDto(2025, 4)),
            calculadoEn = "2026-04-02T09:30:00Z"
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
