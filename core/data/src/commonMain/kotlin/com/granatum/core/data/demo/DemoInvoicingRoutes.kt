package com.granatum.core.data.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * `/api/facturacion…` (ADMIN only): uploads recognised a few seconds later, warnings, versions,
 * quarters, reports and company data, following the backend's specs/004-invoices rules.
 */
class DemoInvoicingRoutes(
    private val state: DemoState,
) {
    private class Line(
        val rate: Long,
        val base: Long,
        val quota: Long,
        val surcharge: Long,
        val cause: String?,
    )

    private class Invoice(
        val id: String,
        var bytes: ByteArray,
        val mime: String,
        val uploadedAt: Instant,
    ) {
        var status = "PENDIENTE_RECONOCER"
        var type: String? = null
        var version = 0
        var issuerName: String? = null
        var issuerTaxId: String? = null
        var recipientName: String? = null
        var recipientTaxId: String? = null
        var number: String? = null
        var date: LocalDate? = null
        var concept: String? = null
        var corrective = false
        var lines: List<Line> = emptyList()
        var withholding = 0L
        var total: Long? = null
        var doubtful: List<String> = emptyList()
        var recognition: String? = null
        var recognizeAt: Instant? = uploadedAt + RECOGNITION_DELAY
        val recognitions = mutableListOf<JsonObject>()
        val changes = mutableListOf<JsonObject>()
    }

    private var companyName: String? = "Granatum Flores SL"
    private var companyTaxId: String? = "B18765432"
    private val invoices = mutableListOf<Invoice>()
    private val quarters = mutableMapOf<Pair<Int, Int>, Boolean>()
    private val quarterEvents = mutableMapOf<Pair<Int, Int>, MutableList<JsonObject>>()

    init {
        val today = state.today()
        val lastQuarter = quarterOf(today.minus(DatePeriod(months = 3)))
        quarters[lastQuarter] = true
        quarterEvents[lastQuarter] = mutableListOf(event("CIERRE", null, state.now() - 864_000.seconds))
        seed(
            "CONFIRMADA",
            "RECIBIDA",
            "Flores del Valle SL",
            "B11223344",
            "FV-2026-118",
            today.minus(DatePeriod(days = 12)),
            listOf(Line(2100, 42_000, 8_820, 0, null)),
            0,
        )
        seed(
            "CONFIRMADA",
            "EMITIDA",
            "Hotel Alhambra SA",
            "A18000017",
            "GF-2026-041",
            today.minus(DatePeriod(days = 8)),
            listOf(Line(1000, 150_000, 15_000, 0, null)),
            0,
        )
        seed(
            "CONFIRMADA",
            "RECIBIDA",
            "Transportes Genil SL",
            "B18999888",
            "TG-882",
            today.minus(DatePeriod(days = 100)),
            listOf(Line(2100, 9_000, 1_890, 0, null)),
            0,
        )
        seed(
            "BORRADOR",
            "RECIBIDA",
            "Cerámicas Levante SL",
            "B46112233",
            "CL-5521",
            today.minus(DatePeriod(days = 3)),
            listOf(Line(2100, 20_000, 4_200, 0, null)),
            0,
            total = 25_000,
            doubtful = listOf("total"),
        )
    }

    private fun seed(
        status: String,
        type: String,
        name: String,
        taxId: String,
        number: String,
        date: LocalDate,
        lines: List<Line>,
        withholding: Long,
        total: Long? = null,
        doubtful: List<String> = emptyList(),
    ) {
        val invoice =
            Invoice(
                state.newId(),
                DemoDocuments.pdf(listOf("Factura de demostracion", "$name - $number", "Fecha: $date")),
                "application/pdf",
                state.now(),
            )
        invoice.status = status
        invoice.type = type
        if (type == "RECIBIDA") {
            invoice.issuerName = name
            invoice.issuerTaxId = taxId
            invoice.recipientName = companyName
            invoice.recipientTaxId = companyTaxId
        } else {
            invoice.issuerName = companyName
            invoice.issuerTaxId = companyTaxId
            invoice.recipientName = name
            invoice.recipientTaxId = taxId
        }
        invoice.number = number
        invoice.date = date
        invoice.lines = lines
        invoice.withholding = withholding
        invoice.total = total ?: (lines.sumOf { it.base + it.quota + it.surcharge } - withholding)
        invoice.doubtful = doubtful
        invoice.recognition = "RECONOCIDA"
        invoice.recognizeAt = null
        invoice.recognitions += recognitionEntry("RECONOCIDA")
        invoices += invoice
    }

    fun route(request: DemoRequest): DemoReply? {
        if (!request.isAdmin) return forbidden
        invoices.forEach { finishRecognition(it) }
        val s = request.segments.drop(1)
        return when (s.firstOrNull()) {
            "empresa" -> company(request)
            "facturas" -> invoices(request, s.drop(1))
            "trimestres" -> quarters(request, s.drop(1))
            "reportes" -> report(request)
            else -> null
        }
    }

    // --- Company ---

    private fun company(request: DemoRequest): DemoReply? =
        when (request.method) {
            HttpMethod.Get -> companyJson() ?: notFound("EMPRESA_SIN_CONFIGURAR")
            HttpMethod.Put -> {
                val taxId =
                    request.body
                        .str("nif")
                        ?.uppercase()
                        ?.filter { it.isLetterOrDigit() }
                        .orEmpty()
                if (taxId.length != 9) {
                    error(HttpStatusCode.UnprocessableEntity, "NIF_INVALIDO", "El NIF no es válido")
                } else {
                    companyName = request.body.str("razonSocial")?.trim()
                    companyTaxId = taxId
                    companyJson()
                }
            }
            else -> null
        }

    private fun companyJson(): DemoReply? {
        val name = companyName ?: return null
        return json(
            buildJsonObject {
                put("razonSocial", name)
                put("nif", companyTaxId)
                put("reconocimientoActivo", true)
            },
        )
    }

    // --- Invoices ---

    private fun invoices(
        request: DemoRequest,
        s: List<String>,
    ): DemoReply? {
        if (s.isEmpty()) return if (request.method == HttpMethod.Get) list(request) else upload(request)
        val invoice = invoices.firstOrNull { it.id == s[0] } ?: return notFound("FACTURA_NOT_FOUND")
        return when (s.getOrNull(1)) {
            null -> if (request.method == HttpMethod.Get) json(invoice.toJson()) else save(invoice, request)
            "confirmar" -> confirm(invoice, request)
            "descartar" -> discard(invoice, request)
            "reconocer" -> {
                if (invoice.status != "PENDIENTE_RECONOCER" && invoice.status != "BORRADOR") return conflict("ESTADO_NO_PERMITIDO")
                invoice.recognizeAt = state.now() + RECOGNITION_DELAY
                DemoReply.Accepted
            }
            "original" -> DemoReply.Bytes(invoice.bytes, invoice.mime, "factura-${invoice.id}.${DemoDocuments.extension(invoice.mime)}")
            "historial" ->
                json(
                    buildJsonObject {
                        put("reconocimientos", JsonArray(invoice.recognitions))
                        put("cambios", JsonArray(invoice.changes))
                    },
                )
            else -> null
        }
    }

    private fun list(request: DemoRequest): DemoReply {
        val text = request.query["parte"]?.lowercase()
        val from = request.query["desde"]?.let { LocalDate.parse(it) }
        val to = request.query["hasta"]?.let { LocalDate.parse(it) }
        val page = request.query["pagina"]?.toIntOrNull() ?: 0
        val size = (request.query["tamano"]?.toIntOrNull() ?: 50).coerceIn(1, 200)
        val filtered =
            invoices
                .filter { invoice ->
                    (request.query["estado"] == null || invoice.status == request.query["estado"]) &&
                        (request.query["tipo"] == null || invoice.type == request.query["tipo"]) &&
                        (from == null || (invoice.date != null && invoice.date!! >= from)) &&
                        (to == null || (invoice.date != null && invoice.date!! <= to)) &&
                        (
                            text == null ||
                                listOfNotNull(
                                    invoice.issuerName,
                                    invoice.issuerTaxId,
                                    invoice.recipientName,
                                    invoice.recipientTaxId,
                                ).any { it.lowercase().contains(text) }
                        )
                }.sortedWith(compareBy<Invoice> { it.date != null }.thenByDescending { it.date }.thenByDescending { it.uploadedAt })
        val elements =
            filtered.drop(page * size).take(size).map { invoice ->
                buildJsonObject {
                    put("id", invoice.id)
                    put("estado", invoice.status)
                    put("tipo", invoice.type)
                    put("emisorNombre", invoice.issuerName)
                    put("emisorNif", invoice.issuerTaxId)
                    put("destinatarioNombre", invoice.recipientName)
                    put("destinatarioNif", invoice.recipientTaxId)
                    put("numero", invoice.number)
                    put("fechaEmision", invoice.date?.toString())
                    put("total", invoice.total?.let { JsonPrimitive(it / 100.0) } ?: JsonNull)
                    put("numeroAvisos", warnings(invoice).size)
                }
            }
        return json(
            buildJsonObject {
                put("elementos", JsonArray(elements))
                put("pagina", page)
                put("tamano", size)
                put("total", filtered.size)
            },
        )
    }

    private fun upload(request: DemoRequest): DemoReply {
        val parts = DemoDocuments.multipart(request.bytes, request.contentType)
        if (parts.isEmpty()) return error(HttpStatusCode.BadRequest, "VALIDACION", "Falta la parte ficheros")
        val results =
            parts.mapIndexed { index, part ->
                val mime = DemoDocuments.sniff(part.bytes)
                val duplicate = invoices.firstOrNull { it.status != "DESCARTADA" && it.bytes.contentEquals(part.bytes) }
                val (result, id) =
                    when {
                        part.bytes.isEmpty() -> "VACIO" to null
                        part.bytes.size > MAX_BYTES -> "DEMASIADO_GRANDE" to null
                        mime == null -> "FORMATO_NO_ADMITIDO" to null
                        duplicate != null -> "DUPLICADA" to duplicate.id
                        else -> "ACEPTADA" to Invoice(state.newId(), part.bytes, mime, state.now()).also { invoices += it }.id
                    }
                buildJsonObject {
                    put("fichero", index + 1)
                    put("resultado", result)
                    put("facturaId", id)
                }
            }
        return json(JsonArray(results), HttpStatusCode.Accepted)
    }

    /** Recognition "finishes" a few seconds after upload, filling the draft as the AI would. */
    private fun finishRecognition(invoice: Invoice) {
        val due = invoice.recognizeAt ?: return
        if (state.now() < due) return
        invoice.recognizeAt = null
        val supplier = SUPPLIERS[invoices.indexOf(invoice) % SUPPLIERS.size]
        invoice.issuerName = supplier.first
        invoice.issuerTaxId = supplier.second
        invoice.recipientName = companyName
        invoice.recipientTaxId = companyTaxId
        invoice.type = "RECIBIDA"
        invoice.number = "R-${(1000..9999).random()}"
        invoice.date = state.today().minus(DatePeriod(days = 1))
        invoice.concept = "Material de floristería"
        invoice.lines = listOf(Line(2100, 12_500, 2_625, 0, null))
        invoice.total = 15_125
        invoice.doubtful = listOf("numero")
        invoice.recognition = "RECONOCIDA"
        invoice.status = "BORRADOR"
        invoice.version++
        invoice.recognitions += recognitionEntry("RECONOCIDA")
    }

    private fun save(
        invoice: Invoice,
        request: DemoRequest,
    ): DemoReply {
        val body = request.body
        if (body.int("version") != invoice.version) return conflict("VERSION_DESACTUALIZADA")
        if (invoice.status == "DESCARTADA") return conflict("ESTADO_NO_PERMITIDO")
        if (invoice.status == "CONFIRMADA" && closed(invoice.date)) return conflict("TRIMESTRE_CERRADO")
        val before = invoice.toJson()
        val issuer = body["emisor"] as? JsonObject
        val recipient = body["destinatario"] as? JsonObject
        val updated =
            Invoice(invoice.id, invoice.bytes, invoice.mime, invoice.uploadedAt).apply {
                type = body.str("tipo")
                issuerName = issuer?.str("nombre")
                issuerTaxId = issuer?.str("nif")
                recipientName = recipient?.str("nombre")
                recipientTaxId = recipient?.str("nif")
                number = body.str("numero")
                date = body.str("fechaEmision")?.let { LocalDate.parse(it) }
                concept = body.str("concepto")
                corrective = body.bool("rectificativa") == true
                lines =
                    (body["lineas"] as? JsonArray).orEmpty().map { element ->
                        val line = element as JsonObject
                        Line(
                            cents(line.str("tipoIva")),
                            cents(line.str("base")),
                            cents(line.str("cuota")),
                            cents(line.str("recargo")),
                            line.str("causaSinCuota"),
                        )
                    }
                withholding = cents(body.str("retenciones"))
                total = body.str("total")?.let { cents(it) }
            }
        if (updated.type == null) updated.type = classify(updated)
        if (invoice.status == "CONFIRMADA" && warnings(updated).any { it.second }) {
            return error(
                HttpStatusCode.UnprocessableEntity,
                "FACTURA_INCOHERENTE",
                warnings(updated)
                    .filter {
                        it.second
                    }.joinToString("; ") { "${it.first}: ${it.third}" },
            )
        }
        invoice.type = updated.type
        invoice.issuerName = updated.issuerName
        invoice.issuerTaxId = updated.issuerTaxId
        invoice.recipientName = updated.recipientName
        invoice.recipientTaxId = updated.recipientTaxId
        invoice.number = updated.number
        invoice.date = updated.date
        invoice.concept = updated.concept
        invoice.corrective = updated.corrective
        invoice.lines = updated.lines
        invoice.withholding = updated.withholding
        invoice.total = updated.total
        if (invoice.status == "PENDIENTE_RECONOCER") {
            invoice.status = "BORRADOR"
            invoice.recognizeAt = null
        }
        if (invoice.status == "CONFIRMADA") invoice.changes += change("CORRECCION", request, before)
        invoice.version++
        return json(invoice.toJson())
    }

    private fun confirm(
        invoice: Invoice,
        request: DemoRequest,
    ): DemoReply {
        if (request.body.int("version") != invoice.version) return conflict("VERSION_DESACTUALIZADA")
        if (invoice.status != "BORRADOR") return conflict("ESTADO_NO_PERMITIDO")
        if (companyName == null) return conflict("EMPRESA_SIN_CONFIGURAR")
        if (closed(invoice.date)) return conflict("TRIMESTRE_CERRADO")
        val blocking = warnings(invoice).filter { it.second }
        if (blocking.any { it.third == "DUPLICADA" }) return conflict("FACTURA_DUPLICADA")
        if (blocking.isNotEmpty()) {
            return error(
                HttpStatusCode.UnprocessableEntity,
                "FACTURA_INCOHERENTE",
                blocking.joinToString("; ") { "${it.first}: ${it.third}" },
            )
        }
        invoice.status = "CONFIRMADA"
        invoice.doubtful = emptyList()
        invoice.version++
        return json(invoice.toJson())
    }

    private fun discard(
        invoice: Invoice,
        request: DemoRequest,
    ): DemoReply {
        if (request.body.int("version") != invoice.version) return conflict("VERSION_DESACTUALIZADA")
        if (invoice.status == "DESCARTADA") return conflict("ESTADO_NO_PERMITIDO")
        if (invoice.status == "CONFIRMADA") {
            if (closed(invoice.date)) return conflict("TRIMESTRE_CERRADO")
            invoice.changes += change("DESCARTE", request, invoice.toJson())
        }
        invoice.status = "DESCARTADA"
        invoice.recognizeAt = null
        invoice.version++
        return json(invoice.toJson())
    }

    /** (field, blocking, code). Simplified from the backend's ValidadorFactura. */
    private fun warnings(invoice: Invoice): List<Triple<String, Boolean, String>> {
        if (invoice.status == "DESCARTADA") return emptyList()
        return buildList {
            fun required(
                field: String,
                value: Any?,
            ) {
                if (value == null || (value is String && value.isBlank())) add(Triple(field, true, "OBLIGATORIO"))
            }
            required("tipo", invoice.type)
            required("emisor.nif", invoice.issuerTaxId)
            required("numero", invoice.number)
            required("fechaEmision", invoice.date)
            required("total", invoice.total)
            if (invoice.lines.isEmpty()) add(Triple("lineas", true, "OBLIGATORIO"))
            listOfNotNull("emisor.nif" to invoice.issuerTaxId, invoice.recipientTaxId?.let { "destinatario.nif" to it }).forEach { (field, value) ->
                if (value != null && value.filter { it.isLetterOrDigit() }.length != 9) add(Triple(field, true, "NIF_INVALIDO"))
            }
            invoice.total?.let { total ->
                if (invoice.lines.isNotEmpty() &&
                    abs(invoice.lines.sumOf { it.base + it.quota + it.surcharge } - invoice.withholding - total) > 1
                ) {
                    add(Triple("total", true, "NO_CUADRA"))
                }
            }
            if (invoice.lines.any { it.quota == 0L && it.cause == null }) add(Triple("lineas", true, "SIN_CAUSA_CUOTA_CERO"))
            if (closed(invoice.date) && invoice.status != "CONFIRMADA") add(Triple("fechaEmision", true, "TRIMESTRE_CERRADO"))
            if (invoices.any {
                    it !== invoice &&
                        it.status == "CONFIRMADA" &&
                        it.issuerTaxId == invoice.issuerTaxId &&
                        it.number == invoice.number &&
                        it.date == invoice.date
                }
            ) {
                add(Triple("numero", true, "DUPLICADA"))
            }
            invoice.date?.let { if (it > state.today()) add(Triple("fechaEmision", false, "FECHA_FUTURA")) }
            if (invoice.status != "CONFIRMADA") invoice.doubtful.forEach { add(Triple(it, false, "DUDOSO")) }
        }
    }

    private fun warningMessage(code: String) =
        when (code) {
            "OBLIGATORIO" -> "Dato obligatorio"
            "NIF_INVALIDO" -> "El NIF no es válido"
            "NO_CUADRA" -> "Las líneas menos las retenciones no suman el total"
            "SIN_CAUSA_CUOTA_CERO" -> "Una línea sin cuota necesita su causa"
            "TRIMESTRE_CERRADO" -> "La fecha cae en un trimestre cerrado"
            "DUPLICADA" -> "Ya hay una factura confirmada con este emisor, número y fecha"
            "FECHA_FUTURA" -> "La fecha es posterior a hoy"
            else -> "El reconocimiento no está seguro de este dato"
        }

    private fun classify(invoice: Invoice): String? =
        when (companyTaxId) {
            null -> null
            invoice.issuerTaxId -> "EMITIDA"
            invoice.recipientTaxId -> "RECIBIDA"
            else -> null
        }

    private fun Invoice.toJson() =
        buildJsonObject {
            put("id", id)
            put("estado", status)
            put("tipo", type)
            put("version", version)
            put(
                "emisor",
                if (issuerName == null &&
                    issuerTaxId == null
                ) {
                    JsonNull
                } else {
                    buildJsonObject {
                        put("nombre", issuerName)
                        put("nif", issuerTaxId)
                    }
                },
            )
            put(
                "destinatario",
                if (recipientName == null &&
                    recipientTaxId == null
                ) {
                    JsonNull
                } else {
                    buildJsonObject {
                        put("nombre", recipientName)
                        put("nif", recipientTaxId)
                    }
                },
            )
            put("numero", number)
            put("fechaEmision", date?.toString())
            put("concepto", concept)
            put("moneda", "EUR")
            put("rectificativa", corrective)
            put(
                "lineas",
                buildJsonArray {
                    lines.forEach { line ->
                        add(
                            buildJsonObject {
                                put("tipoIva", money(line.rate))
                                put("base", money(line.base))
                                put("cuota", money(line.quota))
                                put("recargo", money(line.surcharge))
                                put("causaSinCuota", line.cause)
                            },
                        )
                    }
                },
            )
            put("retenciones", money(withholding))
            put("total", total?.let { money(it) })
            put("trimestreCerrado", closed(date))
            put(
                "reconocimiento",
                recognition?.let { result ->
                    buildJsonObject {
                        put("resultado", result)
                        put("camposDudosos", JsonArray(doubtful.map { JsonPrimitive(it) }))
                    }
                }
                    ?: JsonNull,
            )
            put(
                "avisos",
                JsonArray(
                    warnings(this@toJson).map { (field, blocking, code) ->
                        buildJsonObject {
                            put("campo", field)
                            put("codigo", code)
                            put("bloquea", blocking)
                            put("mensaje", warningMessage(code))
                        }
                    },
                ),
            )
        }

    private fun recognitionEntry(result: String) =
        buildJsonObject {
            put("resultado", result)
            put("modelo", "demo")
            put("creadoEn", state.now().toString())
            put("propuesta", JsonNull)
        }

    private fun change(
        action: String,
        request: DemoRequest,
        before: JsonObject,
    ) = buildJsonObject {
        put("accion", action)
        put("autorId", request.caller?.employeeId)
        put("ocurridoEn", state.now().toString())
        put("valoresAnteriores", JsonObject(before.filterKeys { it in setOf("numero", "fechaEmision", "total", "retenciones", "tipo") }))
    }

    // --- Quarters ---

    private fun quarterOf(date: LocalDate) = date.year to (date.month.number - 1) / 3 + 1

    private fun closed(date: LocalDate?) = date != null && quarters[quarterOf(date)] == true

    private fun event(
        action: String,
        reason: String?,
        at: Instant = state.now(),
    ) = buildJsonObject {
        put("accion", action)
        put("autorId", DemoState.ADMIN_ID)
        put("ocurridoEn", at.toString())
        put("motivo", reason)
    }

    private fun quarterJson(
        year: Int,
        quarter: Int,
    ) = buildJsonObject {
        put("trimestre", quarter)
        put("cerrado", quarters[year to quarter] == true)
        put("eventos", JsonArray(quarterEvents[year to quarter].orEmpty()))
    }

    private fun quarters(
        request: DemoRequest,
        s: List<String>,
    ): DemoReply? {
        if (s.isEmpty()) {
            val year = request.query["anio"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDACION", "anio")
            return json(JsonArray((1..4).map { quarterJson(year, it) }))
        }
        val year = s[0].toIntOrNull() ?: return null
        val quarter =
            s.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..4 }
                ?: return error(HttpStatusCode.UnprocessableEntity, "PERIODO_INVALIDO", "Trimestre")
        val key = year to quarter
        return when (s.getOrNull(2)) {
            "cerrar" -> {
                if (quarters[key] == true) return conflict("TRIMESTRE_CERRADO")
                val pending =
                    invoices.count {
                        (it.status == "BORRADOR" || it.status == "PENDIENTE_RECONOCER") &&
                            it.date?.let(::quarterOf) == key
                    }
                if (pending >
                    0
                ) {
                    return error(
                        HttpStatusCode.Conflict,
                        "TRIMESTRE_CON_PENDIENTES",
                        "Quedan $pending facturas sin confirmar en el trimestre",
                    )
                }
                quarters[key] = true
                quarterEvents.getOrPut(key) { mutableListOf() } += event("CIERRE", null)
                json(quarterJson(year, quarter))
            }
            "reabrir" -> {
                if (quarters[key] != true) return conflict("TRIMESTRE_ABIERTO")
                val reason =
                    request.body
                        .str("motivo")
                        ?.trim()
                        .orEmpty()
                if (reason.length !in 10..500) return error(HttpStatusCode.BadRequest, "VALIDACION", "motivo")
                quarters[key] = false
                quarterEvents.getOrPut(key) { mutableListOf() } += event("REAPERTURA", reason)
                json(quarterJson(year, quarter))
            }
            else -> null
        }
    }

    // --- Reports ---

    private fun report(request: DemoRequest): DemoReply {
        val period = request.query["periodo"]?.uppercase()
        val year = request.query["anio"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDACION", "anio")
        val (from, to, label) =
            when (period) {
                "MENSUAL" -> {
                    val month = request.query["mes"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDACION", "mes")
                    val start = LocalDate(year, month, 1)
                    Triple(start, start.plusMonths(1).minus(DatePeriod(days = 1)), "$year-${month.toString().padStart(2, '0')}")
                }
                "TRIMESTRAL" -> {
                    val quarter =
                        request.query["trimestre"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDACION", "trimestre")
                    val start = LocalDate(year, (quarter - 1) * 3 + 1, 1)
                    Triple(start, start.plusMonths(3).minus(DatePeriod(days = 1)), "$year-T$quarter")
                }
                "ANUAL" -> Triple(LocalDate(year, 1, 1), LocalDate(year, 12, 31), "$year")
                else -> return error(HttpStatusCode.UnprocessableEntity, "PERIODO_INVALIDO", "Periodo")
            }
        val inPeriod = invoices.filter { it.date != null && it.date!! in from..to }
        val confirmed = inPeriod.filter { it.status == "CONFIRMADA" }

        fun group(type: String): JsonObject {
            val list = confirmed.filter { it.type == type }
            val sign = { invoice: Invoice -> if (invoice.corrective) -1 else 1 }
            val lines = list.flatMap { invoice -> invoice.lines.map { sign(invoice) to it } }
            return buildJsonObject {
                put("facturas", list.size)
                put("base", money(lines.sumOf { (s, l) -> s * l.base }))
                put(
                    "ivaPorTipo",
                    JsonObject(
                        lines.groupBy { it.second.rate }.mapKeys { money(it.key) }.mapValues { (_, v) ->
                            JsonPrimitive(
                                money(
                                    v.sumOf { (s, l) ->
                                        s *
                                            l.quota
                                    },
                                ),
                            )
                        },
                    ),
                )
                put("recargo", money(lines.sumOf { (s, l) -> s * l.surcharge }))
                put("retenciones", money(list.sumOf { sign(it) * it.withholding }))
                put("total", money(list.sumOf { sign(it) * (it.total ?: 0) }))
                put(
                    "sinCuota",
                    JsonObject(
                        lines.filter { it.second.quota == 0L && it.second.cause != null }.groupBy { it.second.cause!! }.mapValues { (_, v) ->
                            JsonPrimitive(
                                money(
                                    v.sumOf { (s, l) ->
                                        s *
                                            l.base
                                    },
                                ),
                            )
                        },
                    ),
                )
            }
        }
        val issued = group("EMITIDA")
        val received = group("RECIBIDA")
        val pending = inPeriod.count { it.status == "BORRADOR" || it.status == "PENDIENTE_RECONOCER" }
        return when (request.query["formato"]?.lowercase() ?: "json") {
            "csv" ->
                DemoReply.Bytes(
                    (
                        "Tipo;Facturas;Base;Retenciones;Total\r\n" +
                            "Emitidas;${issued.str(
                                "facturas",
                            )};${issued.str(
                                "base",
                            )?.replace(
                                '.',
                                ',',
                            )};${issued.str("retenciones")?.replace('.', ',')};${issued.str("total")?.replace('.', ',')}\r\n" +
                            "Recibidas;${received.str(
                                "facturas",
                            )};${received.str(
                                "base",
                            )?.replace(
                                '.',
                                ',',
                            )};${received.str("retenciones")?.replace('.', ',')};${received.str("total")?.replace('.', ',')}\r\n"
                    ).encodeToByteArray(),
                    "text/csv",
                    "reporte-facturacion_$label.csv",
                )
            "pdf" ->
                DemoReply.Bytes(
                    DemoDocuments.pdf(
                        listOf(
                            "Reporte de facturacion $label (demo)",
                            "Del $from al $to",
                            "",
                            "Emitidas: ${issued.str(
                                "facturas",
                            )} facturas, base ${issued.str("base")} EUR, total ${issued.str("total")} EUR",
                            "Recibidas: ${received.str(
                                "facturas",
                            )} facturas, base ${received.str("base")} EUR, total ${received.str("total")} EUR",
                            "Pendientes de confirmar: $pending",
                        ),
                    ),
                    "application/pdf",
                    "reporte-facturacion_$label.pdf",
                )
            "json" ->
                json(
                    buildJsonObject {
                        put(
                            "periodo",
                            buildJsonObject {
                                put("tipo", period)
                                put("anio", year)
                                request.query["mes"]?.toIntOrNull()?.let { put("mes", it) }
                                request.query["trimestre"]?.toIntOrNull()?.let { put("trimestre", it) }
                                put("desde", from.toString())
                                put("hasta", to.toString())
                            },
                        )
                        put("emitidas", issued)
                        put("recibidas", received)
                        put("pendientes", pending)
                        put(
                            "trimestresCerrados",
                            JsonArray(
                                quarters.filter { it.value && it.key.first == year }.keys.map { (y, q) ->
                                    buildJsonObject {
                                        put("anio", y)
                                        put("trimestre", q)
                                        put("desde", LocalDate(y, (q - 1) * 3 + 1, 1).toString())
                                    }
                                },
                            ),
                        )
                        put("calculadoEn", state.now().toString())
                    },
                )
            else -> error(HttpStatusCode.UnprocessableEntity, "PERIODO_INVALIDO", "Formato")
        }
    }

    private fun LocalDate.plusMonths(months: Int): LocalDate {
        val index = year * 12 + (month.number - 1) + months
        return LocalDate(index / 12, index % 12 + 1, 1)
    }

    private fun conflict(code: String) = error(HttpStatusCode.Conflict, code, code)

    private companion object {
        val RECOGNITION_DELAY = 6.seconds
        const val MAX_BYTES = 10 * 1024 * 1024
        val SUPPLIERS =
            listOf(
                "Flores del Valle SL" to "B11223344",
                "Viveros Sierra Nevada SL" to "B18223344",
                "Papelería Albaicín SL" to "B18556677",
            )

        fun money(cents: Long): String {
            val sign = if (cents < 0) "-" else ""
            val absolute = abs(cents)
            return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
        }

        fun cents(text: String?): Long {
            val raw = text?.trim().orEmpty().ifEmpty { "0" }
            val negative = raw.startsWith("-")
            val (whole, decimals) = raw.removePrefix("-").split('.').let { it[0] to it.getOrElse(1) { "" } }
            val value = (whole.toLongOrNull() ?: 0) * 100 + decimals.padEnd(2, '0').take(2).toLong()
            return if (negative) -value else value
        }
    }
}
