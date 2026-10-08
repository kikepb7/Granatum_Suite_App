package com.granatum.feature.invoicing.data

import com.granatum.core.domain.util.Result
import com.granatum.feature.invoicing.data.remote.InvoicingRemoteDataSource
import com.granatum.feature.invoicing.data.repository.KtorInvoicingRepository
import com.granatum.feature.invoicing.domain.model.InvoiceFilter
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.InvoicingError
import com.granatum.feature.invoicing.domain.model.Money
import com.granatum.feature.invoicing.domain.model.NoQuotaCause
import com.granatum.feature.invoicing.domain.model.Party
import com.granatum.feature.invoicing.domain.model.ReportFormat
import com.granatum.feature.invoicing.domain.model.ReportPeriod
import com.granatum.feature.invoicing.domain.model.UploadDocument
import com.granatum.feature.invoicing.domain.model.UploadOutcome
import com.granatum.feature.invoicing.domain.model.VatLine
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** specs/008-facturacion, contracts/facturacion-api.md. */
class InvoicingRepositoryTest {

    private class Sent(val method: String, val path: String, val query: String, val contentType: String?, val body: String)

    private val sent = mutableListOf<Sent>()

    private fun repo(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): KtorInvoicingRepository {
        val client = HttpClient(MockEngine { request ->
            sent += Sent(
                request.method.value,
                request.url.encodedPath.substringAfter("/api"),
                request.url.encodedQuery,
                request.body.contentType?.toString(),
                request.body.toByteArray().decodeToString()
            )
            handler(request)
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout)
            defaultRequest { contentType(ContentType.Application.Json) }
        }
        return KtorInvoicingRepository(InvoicingRemoteDataSource(client))
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.error(code: String, status: HttpStatusCode, message: String = "") =
        json("""{"code":"$code","message":"$message"}""", status)

    private val invoiceJson = """
        {"id":"f1","estado":"BORRADOR","tipo":"RECIBIDA","version":3,
         "emisor":{"nombre":"Flores SL","nif":"B12345678"},"destinatario":null,
         "numero":"A-1","fechaEmision":"2026-10-01","concepto":null,"moneda":"EUR","rectificativa":false,
         "lineas":[{"tipoIva":"21.00","base":"100.00","cuota":"21.00","recargo":"0.00","causaSinCuota":null},
                   {"tipoIva":"0.00","base":"10.00","cuota":"0.00","recargo":"0.00","causaSinCuota":"EXENTA"}],
         "retenciones":"15.00","total":"116.00","trimestreCerrado":false,
         "reconocimiento":{"resultado":"RECONOCIDA","camposDudosos":["numero"]},
         "avisos":[{"campo":"total","codigo":"NO_CUADRA","bloquea":true,"mensaje":"No cuadra"}]}
    """.trimIndent()

    @Test
    fun an_invoice_is_read_with_amounts_in_cents() = runTest {
        val result = repo { json(invoiceJson) }.invoice("f1")
        val invoice = assertIs<Result.Success<*>>(result).data as com.granatum.feature.invoicing.domain.model.Invoice
        assertEquals(InvoiceState.DRAFT, invoice.state)
        assertEquals(InvoiceType.RECEIVED, invoice.type)
        assertEquals(Money(11600), invoice.total)
        assertEquals(Money(1500), invoice.withholding)
        assertEquals(NoQuotaCause.EXEMPT, invoice.lines[1].noQuotaCause)
        assertEquals(listOf("numero"), invoice.recognition?.doubtfulFields)
        assertTrue(invoice.hasBlockingWarnings)
        assertEquals(LocalDate(2026, 10, 1), invoice.issueDate)
    }

    @Test
    fun a_save_sends_every_field_with_its_version() = runTest {
        val repository = repo { json(invoiceJson) }
        val draft = (repository.invoice("f1") as Result.Success).data.draft().copy(
            recipient = Party(name = "", taxId = " "),
            lines = listOf(VatLine(rate = Money(2100), base = Money(10000), quota = Money(2100))),
            total = Money(10600)
        )
        repository.save("f1", draft)

        val put = sent.last()
        assertEquals("PUT /facturacion/facturas/f1", "${put.method} ${put.path}")
        val body = Json.parseToJsonElement(put.body).toString()
        assertTrue(""""version":3""" in body, body)
        assertTrue(""""tipo":"RECIBIDA"""" in body)
        assertTrue(""""emisor":{"nombre":"Flores SL","nif":"B12345678"}""" in body)
        assertTrue(""""destinatario":null""" in body, "an empty party goes as null")
        assertTrue(""""lineas":[{"tipoIva":"21.00","base":"100.00","cuota":"21.00","recargo":"0.00","causaSinCuota":null}]""" in body, body)
        assertTrue(""""retenciones":"15.00"""" in body)
        assertTrue(""""total":"106.00"""" in body)
        assertTrue(""""fechaEmision":"2026-10-01"""" in body)
    }

    @Test
    fun confirm_and_discard_send_the_version() = runTest {
        val repository = repo { json(invoiceJson) }
        repository.confirm("f1", 3)
        repository.discard("f1", 4)
        assertEquals("""{"version":3}""", sent[0].body)
        assertEquals("/facturacion/facturas/f1/confirmar", sent[0].path)
        assertEquals("""{"version":4}""", sent[1].body)
        assertEquals("/facturacion/facturas/f1/descartar", sent[1].path)
    }

    @Test
    fun the_list_sends_its_filters_and_reads_the_numeric_total() = runTest {
        val page = repo {
            json(
                """{"elementos":[{"id":"f1","estado":"CONFIRMADA","tipo":"EMITIDA","emisorNombre":"Yo","emisorNif":"B1",
                "destinatarioNombre":"Cliente","destinatarioNif":"12345678Z","numero":"7","fechaEmision":"2026-09-30",
                "total":1210.5,"numeroAvisos":0},{"id":"f2","estado":"PENDIENTE_RECONOCER","total":null,"numeroAvisos":2}],
                "pagina":0,"tamano":50,"total":51}"""
            )
        }.invoices(InvoiceFilter(state = InvoiceState.CONFIRMED, type = InvoiceType.ISSUED, from = LocalDate(2026, 7, 1), text = " cliente "), page = 0)

        val data = assertIs<Result.Success<com.granatum.feature.invoicing.domain.model.Page<com.granatum.feature.invoicing.domain.model.InvoiceSummary>>>(page).data
        assertEquals(Money(121050), data.items[0].total)
        assertEquals("Cliente", data.items[0].counterparty?.name)
        assertNull(data.items[1].total)
        assertTrue(data.hasMore)
        val query = sent.single().query
        assertTrue("estado=CONFIRMADA" in query && "tipo=EMITIDA" in query && "desde=2026-07-01" in query, query)
        assertTrue("parte=cliente" in query && "pagina=0" in query && "tamano=50" in query, query)
        assertTrue("hasta" !in query)
    }

    @Test
    fun an_upload_is_multipart_with_one_part_per_file() = runTest {
        val results = repo {
            json("""[{"fichero":1,"resultado":"ACEPTADA","facturaId":"n1"},{"fichero":2,"resultado":"DUPLICADA","facturaId":"f1"},{"fichero":3,"resultado":"FORMATO_NO_ADMITIDO","facturaId":null}]""", HttpStatusCode.Accepted)
        }.upload(
            listOf(
                UploadDocument(byteArrayOf(1, 2), "foto.jpg", "image/jpeg"),
                UploadDocument(byteArrayOf(3), "fac\"tura.pdf", "application/pdf"),
                UploadDocument(byteArrayOf(4), "nota.txt", "text/plain")
            )
        )
        val list = assertIs<Result.Success<List<com.granatum.feature.invoicing.domain.model.UploadResult>>>(results).data
        assertEquals(listOf(UploadOutcome.ACCEPTED, UploadOutcome.DUPLICATE, UploadOutcome.UNSUPPORTED_FORMAT), list.map { it.outcome })
        assertEquals("f1", list[1].invoiceId)

        val request = sent.single()
        assertEquals("POST /facturacion/facturas", "${request.method} ${request.path}")
        assertTrue(request.contentType!!.startsWith("multipart/form-data"), request.contentType)
        assertEquals(3, Regex("""name="?ficheros"?""").findAll(request.body).count())
        assertTrue("""filename="fac_tura.pdf"""" in request.body, "quotes in names are neutralised")
        assertTrue("Content-Type: application/pdf" in request.body)
    }

    @Test
    fun the_server_codes_become_typed_errors() = runTest {
        suspend fun errorFor(code: String, status: HttpStatusCode, message: String = "") =
            (repo { error(code, status, message) }.confirm("f1", 0) as Result.Failure).error

        assertEquals(InvoicingError.StaleVersion, errorFor("VERSION_DESACTUALIZADA", HttpStatusCode.Conflict))
        assertEquals(InvoicingError.QuarterClosed, errorFor("TRIMESTRE_CERRADO", HttpStatusCode.Conflict))
        assertEquals(InvoicingError.CompanyNotConfigured, errorFor("EMPRESA_SIN_CONFIGURAR", HttpStatusCode.Conflict))
        assertEquals(InvoicingError.Duplicate, errorFor("FACTURA_DUPLICADA", HttpStatusCode.Conflict))
        assertEquals(InvoicingError.Incoherent("total: NO_CUADRA"), errorFor("FACTURA_INCOHERENTE", HttpStatusCode.UnprocessableEntity, "total: NO_CUADRA"))
        assertEquals(InvoicingError.Forbidden, errorFor("FORBIDDEN", HttpStatusCode.Forbidden))
        assertEquals(InvoicingError.TooLarge, errorFor("PETICION_DEMASIADO_GRANDE", HttpStatusCode.PayloadTooLarge))
    }

    @Test
    fun an_unconfigured_company_is_null_not_a_failure() = runTest {
        assertEquals(Result.Success(null), repo { error("EMPRESA_SIN_CONFIGURAR", HttpStatusCode.NotFound) }.company())
        val company = (repo { json("""{"razonSocial":"Granatum Flores SL","nif":"B12345678","reconocimientoActivo":true}""") }.company() as Result.Success).data
        assertEquals(true, company?.recognitionEnabled)
    }

    @Test
    fun originals_and_report_files_keep_their_bytes_type_and_name() = runTest {
        val bytes = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        val repository = repo { request ->
            val disposition = if (request.url.encodedPath.endsWith("original")) "attachment; filename=\"factura-f1.pdf\"" else "attachment; filename=\"reporte-facturacion_2026-T3.csv\""
            val type = if (request.url.encodedPath.endsWith("original")) "application/pdf" else "text/csv;charset=UTF-8"
            respond(bytes, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType to listOf(type), HttpHeaders.ContentDisposition to listOf(disposition)))
        }
        val original = (repository.original("f1") as Result.Success).data
        assertContentEquals(bytes, original.bytes)
        assertEquals("factura-f1.pdf", original.fileName)
        assertEquals("application/pdf", original.mimeType)

        val csv = (repository.reportFile(ReportPeriod.Quarterly(2026, 3), ReportFormat.CSV) as Result.Success).data
        assertEquals("text/csv", csv.mimeType)
        assertEquals("reporte-facturacion_2026-T3.csv", csv.fileName)
        val query = sent.last().query
        assertTrue("periodo=TRIMESTRAL" in query && "anio=2026" in query && "trimestre=3" in query && "formato=csv" in query, query)
    }

    @Test
    fun the_report_reads_groups_and_rates() = runTest {
        val report = (
            repo {
                json(
                    """{"periodo":{"tipo":"TRIMESTRAL","anio":2026,"trimestre":3,"desde":"2026-07-01","hasta":"2026-09-30"},
                    "emitidas":{"facturas":2,"base":"200.00","ivaPorTipo":{"21.00":"42.00","10.00":"1.00"},"recargo":"0.00","retenciones":"0.00","total":"243.00","sinCuota":{"EXENTA":"5.00"}},
                    "recibidas":{"facturas":0,"base":"0.00","ivaPorTipo":{},"recargo":"0.00","retenciones":"0.00","total":"0.00","sinCuota":{}},
                    "pendientes":1,"trimestresCerrados":[{"anio":2026,"trimestre":3,"desde":"2026-07-01"}],"calculadoEn":"2026-10-09T10:00:00Z"}"""
                )
            }.report(ReportPeriod.Quarterly(2026, 3)) as Result.Success
            ).data
        assertEquals(listOf("10.00", "21.00"), report.issued.vatByRate.keys.toList())
        assertEquals(Money(24300), report.issued.total)
        assertEquals(Money(500), report.issued.noQuota[NoQuotaCause.EXEMPT])
        assertEquals(1, report.pending)
        assertEquals(listOf(2026 to 3), report.closedQuarters)
    }

    @Test
    fun reopening_sends_the_reason_and_quarters_come_in_order() = runTest {
        val repository = repo { request ->
            if (request.url.encodedPath.endsWith("reabrir")) {
                json("""{"trimestre":2,"cerrado":false,"eventos":[{"accion":"CIERRE","autorId":"a","ocurridoEn":"2026-07-02T08:00:00Z"},{"accion":"REAPERTURA","autorId":"a","ocurridoEn":"2026-07-03T08:00:00Z","motivo":"Falta una factura"}]}""")
            } else {
                json("""[{"trimestre":2,"cerrado":true,"eventos":[]},{"trimestre":1,"cerrado":true,"eventos":[]},{"trimestre":4,"cerrado":false,"eventos":[]},{"trimestre":3,"cerrado":false,"eventos":[]}]""")
            }
        }
        assertEquals(listOf(1, 2, 3, 4), (repository.quarters(2026) as Result.Success).data.map { it.quarter })
        val reopened = (repository.reopenQuarter(2026, 2, "  Falta una factura ") as Result.Success).data
        assertEquals("""{"motivo":"Falta una factura"}""", sent.last().body)
        assertEquals("Falta una factura", reopened.events.first().reason)
    }
}
