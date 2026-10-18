package com.granatum.core.demo

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
 * `/api/invoicing…` (ADMIN only): uploads recognised a few seconds later, warnings, versions,
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
        var status = "PENDING_RECOGNITION"
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
        var uncertainFields: List<String> = emptyList()
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

    /** Confirmed invoices issued in a month: the rows of its receipts tab (backend spec 012). */
    internal fun confirmedIn(
        year: Int,
        month: Int,
    ): Int = invoices.count { it.status == "CONFIRMED" && it.date?.year == year && it.date?.month?.number == month }

    init {
        val today = state.today()
        val lastQuarter = quarterOf(today.minus(DatePeriod(months = 3)))
        quarters[lastQuarter] = true
        quarterEvents[lastQuarter] = mutableListOf(event("CLOSE", null, state.now() - 864_000.seconds))
        seed(
            "CONFIRMED",
            "RECEIVED",
            "Flores del Valle SL",
            "B11223344",
            "FV-2026-118",
            today.minus(DatePeriod(days = 12)),
            listOf(Line(2100, 42_000, 8_820, 0, null)),
            0,
        )
        seed(
            "CONFIRMED",
            "ISSUED",
            "Hotel Alhambra SA",
            "A18000017",
            "GF-2026-041",
            today.minus(DatePeriod(days = 8)),
            listOf(Line(1000, 150_000, 15_000, 0, null)),
            0,
        )
        seed(
            "CONFIRMED",
            "RECEIVED",
            "Transportes Genil SL",
            "B18999888",
            "TG-882",
            today.minus(DatePeriod(days = 100)),
            listOf(Line(2100, 9_000, 1_890, 0, null)),
            0,
        )
        seed(
            "DRAFT",
            "RECEIVED",
            "Cerámicas Levante SL",
            "B46112233",
            "CL-5521",
            today.minus(DatePeriod(days = 3)),
            listOf(Line(2100, 20_000, 4_200, 0, null)),
            0,
            total = 25_000,
            uncertainFields = listOf("total"),
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
        uncertainFields: List<String> = emptyList(),
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
        if (type == "RECEIVED") {
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
        invoice.uncertainFields = uncertainFields
        invoice.recognition = "RECOGNIZED"
        invoice.recognizeAt = null
        invoice.recognitions += recognitionEntry("RECOGNIZED")
        invoices += invoice
    }

    fun route(request: DemoRequest): DemoReply? {
        if (!request.isAdmin) return forbidden
        invoices.forEach { finishRecognition(it) }
        val s = request.segments.drop(1)
        return when (s.firstOrNull()) {
            "company" -> company(request)
            "invoices" -> invoices(request, s.drop(1))
            "quarters" -> quarters(request, s.drop(1))
            "reports" -> report(request)
            else -> null
        }
    }

    // --- Company ---

    private fun company(request: DemoRequest): DemoReply? =
        when (request.method) {
            HttpMethod.Get -> companyJson() ?: notFound("COMPANY_NOT_CONFIGURED")
            HttpMethod.Put -> {
                val taxId =
                    request.body
                        .str("nif")
                        ?.uppercase()
                        ?.filter { it.isLetterOrDigit() }
                        .orEmpty()
                if (taxId.length != 9) {
                    error(HttpStatusCode.UnprocessableEntity, "INVALID_NIF", "El NIF no es válido")
                } else {
                    companyName = request.body.str("legalName")?.trim()
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
                put("legalName", name)
                put("nif", companyTaxId)
                put("recognitionActive", true)
            },
        )
    }

    // --- Invoices ---

    private fun invoices(
        request: DemoRequest,
        s: List<String>,
    ): DemoReply? {
        if (s.isEmpty()) return if (request.method == HttpMethod.Get) list(request) else upload(request)
        val invoice = invoices.firstOrNull { it.id == s[0] } ?: return notFound("INVOICE_NOT_FOUND")
        return when (s.getOrNull(1)) {
            null -> if (request.method == HttpMethod.Get) json(invoice.toJson()) else save(invoice, request)
            "confirm" -> confirm(invoice, request)
            "discard" -> discard(invoice, request)
            "recognize" -> {
                if (invoice.status != "PENDING_RECOGNITION" && invoice.status != "DRAFT") return conflict("STATUS_NOT_ALLOWED")
                invoice.recognizeAt = state.now() + RECOGNITION_DELAY
                DemoReply.Accepted
            }
            "original" -> DemoReply.Bytes(invoice.bytes, invoice.mime, "factura-${invoice.id}.${DemoDocuments.extension(invoice.mime)}")
            "history" ->
                json(
                    buildJsonObject {
                        put("recognitions", JsonArray(invoice.recognitions))
                        put("changes", JsonArray(invoice.changes))
                    },
                )
            else -> null
        }
    }

    private fun list(request: DemoRequest): DemoReply {
        val text = request.query["party"]?.lowercase()
        val from = request.query["from"]?.let { LocalDate.parse(it) }
        val to = request.query["to"]?.let { LocalDate.parse(it) }
        val page = request.query["page"]?.toIntOrNull() ?: 0
        val size = (request.query["size"]?.toIntOrNull() ?: 50).coerceIn(1, 200)
        val filtered =
            invoices
                .filter { invoice ->
                    (request.query["status"] == null || invoice.status == request.query["status"]) &&
                        (request.query["type"] == null || invoice.type == request.query["type"]) &&
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
                    put("status", invoice.status)
                    put("type", invoice.type)
                    put("issuerName", invoice.issuerName)
                    put("issuerNif", invoice.issuerTaxId)
                    put("recipientName", invoice.recipientName)
                    put("recipientNif", invoice.recipientTaxId)
                    put("number", invoice.number)
                    put("issueDate", invoice.date?.toString())
                    put("total", invoice.total?.let { JsonPrimitive(it / 100.0) } ?: JsonNull)
                    put("noticeCount", notices(invoice).size)
                }
            }
        return json(
            buildJsonObject {
                put("items", JsonArray(elements))
                put("page", page)
                put("size", size)
                put("total", filtered.size)
            },
        )
    }

    private fun upload(request: DemoRequest): DemoReply {
        val parts = DemoDocuments.multipart(request.bytes, request.contentType).filter { it.name == "files" }
        if (parts.isEmpty()) return error(HttpStatusCode.BadRequest, "VALIDATION", "Falta la parte files")
        val results =
            parts.mapIndexed { index, part ->
                val mime = DemoDocuments.sniff(part.bytes)
                val duplicate = invoices.firstOrNull { it.status != "DISCARDED" && it.bytes.contentEquals(part.bytes) }
                val (result, id) =
                    when {
                        part.bytes.isEmpty() -> "EMPTY" to null
                        part.bytes.size > MAX_BYTES -> "TOO_LARGE" to null
                        mime == null -> "UNSUPPORTED_FORMAT" to null
                        duplicate != null -> "DUPLICATE" to duplicate.id
                        else -> "ACCEPTED" to Invoice(state.newId(), part.bytes, mime, state.now()).also { invoices += it }.id
                    }
                buildJsonObject {
                    put("file", index + 1)
                    put("result", result)
                    put("invoiceId", id)
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
        invoice.type = "RECEIVED"
        invoice.number = "R-${(1000..9999).random()}"
        invoice.date = state.today().minus(DatePeriod(days = 1))
        invoice.concept = "Material de floristería"
        invoice.lines = listOf(Line(2100, 12_500, 2_625, 0, null))
        invoice.total = 15_125
        invoice.uncertainFields = listOf("number")
        invoice.recognition = "RECOGNIZED"
        invoice.status = "DRAFT"
        invoice.version++
        invoice.recognitions += recognitionEntry("RECOGNIZED")
    }

    private fun save(
        invoice: Invoice,
        request: DemoRequest,
    ): DemoReply {
        val body = request.body
        if (body.int("version") != invoice.version) return conflict("STALE_VERSION")
        if (invoice.status == "DISCARDED") return conflict("STATUS_NOT_ALLOWED")
        if (invoice.status == "CONFIRMED" && closed(invoice.date)) return conflict("QUARTER_CLOSED")
        val before = invoice.toJson()
        val issuer = body["issuer"] as? JsonObject
        val recipient = body["recipient"] as? JsonObject
        val updated =
            Invoice(invoice.id, invoice.bytes, invoice.mime, invoice.uploadedAt).apply {
                type = body.str("type")
                issuerName = issuer?.str("name")
                issuerTaxId = issuer?.str("nif")
                recipientName = recipient?.str("name")
                recipientTaxId = recipient?.str("nif")
                number = body.str("number")
                date = body.str("issueDate")?.let { LocalDate.parse(it) }
                concept = body.str("concept")
                corrective = body.bool("corrective") == true
                lines =
                    (body["vatLines"] as? JsonArray).orEmpty().map { element ->
                        val line = element as JsonObject
                        Line(
                            cents(line.str("vatRate")),
                            cents(line.str("base")),
                            cents(line.str("vatAmount")),
                            cents(line.str("surcharge")),
                            line.str("noVatCause"),
                        )
                    }
                withholding = cents(body.str("withholdings"))
                total = body.str("total")?.let { cents(it) }
            }
        if (updated.type == null) updated.type = classify(updated)
        if (invoice.status == "CONFIRMED" && notices(updated).any { it.second }) {
            return error(
                HttpStatusCode.UnprocessableEntity,
                "INCONSISTENT_INVOICE",
                notices(updated)
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
        if (invoice.status == "PENDING_RECOGNITION") {
            invoice.status = "DRAFT"
            invoice.recognizeAt = null
        }
        if (invoice.status == "CONFIRMED") invoice.changes += change("CORRECTION", request, before)
        invoice.version++
        return json(invoice.toJson())
    }

    private fun confirm(
        invoice: Invoice,
        request: DemoRequest,
    ): DemoReply {
        if (request.body.int("version") != invoice.version) return conflict("STALE_VERSION")
        if (invoice.status != "DRAFT") return conflict("STATUS_NOT_ALLOWED")
        if (companyName == null) return conflict("COMPANY_NOT_CONFIGURED")
        if (closed(invoice.date)) return conflict("QUARTER_CLOSED")
        val blocking = notices(invoice).filter { it.second }
        if (blocking.any { it.third == "DUPLICATE" }) return conflict("DUPLICATE_INVOICE")
        if (blocking.isNotEmpty()) {
            return error(
                HttpStatusCode.UnprocessableEntity,
                "INCONSISTENT_INVOICE",
                blocking.joinToString("; ") { "${it.first}: ${it.third}" },
            )
        }
        invoice.status = "CONFIRMED"
        invoice.uncertainFields = emptyList()
        invoice.version++
        return json(invoice.toJson())
    }

    private fun discard(
        invoice: Invoice,
        request: DemoRequest,
    ): DemoReply {
        if (request.body.int("version") != invoice.version) return conflict("STALE_VERSION")
        if (invoice.status == "DISCARDED") return conflict("STATUS_NOT_ALLOWED")
        if (invoice.status == "CONFIRMED") {
            if (closed(invoice.date)) return conflict("QUARTER_CLOSED")
            invoice.changes += change("DISCARD", request, invoice.toJson())
        }
        invoice.status = "DISCARDED"
        invoice.recognizeAt = null
        invoice.version++
        return json(invoice.toJson())
    }

    /** (field, blocking, code). Simplified from the backend's InvoiceValidator. */
    private fun notices(invoice: Invoice): List<Triple<String, Boolean, String>> {
        if (invoice.status == "DISCARDED") return emptyList()
        return buildList {
            fun required(
                field: String,
                value: Any?,
            ) {
                if (value == null || (value is String && value.isBlank())) add(Triple(field, true, "REQUIRED"))
            }
            required("type", invoice.type)
            required("issuer.nif", invoice.issuerTaxId)
            required("number", invoice.number)
            required("issueDate", invoice.date)
            required("total", invoice.total)
            if (invoice.lines.isEmpty()) add(Triple("vatLines", true, "REQUIRED"))
            listOfNotNull("issuer.nif" to invoice.issuerTaxId, invoice.recipientTaxId?.let { "recipient.nif" to it }).forEach { (field, value) ->
                if (value != null && value.filter { it.isLetterOrDigit() }.length != 9) add(Triple(field, true, "INVALID_NIF"))
            }
            invoice.total?.let { total ->
                if (invoice.lines.isNotEmpty() &&
                    abs(invoice.lines.sumOf { it.base + it.quota + it.surcharge } - invoice.withholding - total) > 1
                ) {
                    add(Triple("total", true, "TOTAL_MISMATCH"))
                }
            }
            invoice.lines.forEachIndexed { index, line ->
                if (line.quota == 0L && line.cause == null) add(Triple("vatLines[$index].noVatCause", true, "MISSING_NO_VAT_CAUSE"))
            }
            if (closed(invoice.date) && invoice.status != "CONFIRMED") add(Triple("issueDate", true, "QUARTER_CLOSED"))
            if (invoices.any {
                    it !== invoice &&
                        it.status == "CONFIRMED" &&
                        it.issuerTaxId == invoice.issuerTaxId &&
                        it.number == invoice.number &&
                        it.date == invoice.date
                }
            ) {
                add(Triple("number", true, "DUPLICATE"))
            }
            invoice.date?.let { if (it > state.today()) add(Triple("issueDate", false, "FUTURE_DATE")) }
            if (invoice.status != "CONFIRMED") invoice.uncertainFields.forEach { add(Triple(it, false, "UNCERTAIN")) }
        }
    }

    private fun noticeMessage(code: String) =
        when (code) {
            "REQUIRED" -> "Dato obligatorio"
            "INVALID_NIF" -> "El NIF no es válido"
            "TOTAL_MISMATCH" -> "Las líneas menos las retenciones no suman el total"
            "MISSING_NO_VAT_CAUSE" -> "Una línea sin cuota necesita su causa"
            "QUARTER_CLOSED" -> "La fecha cae en un trimestre cerrado"
            "DUPLICATE" -> "Ya hay una factura confirmada con este emisor, número y fecha"
            "FUTURE_DATE" -> "La fecha es posterior a hoy"
            else -> "El reconocimiento no está seguro de este dato"
        }

    private fun classify(invoice: Invoice): String? =
        when (companyTaxId) {
            null -> null
            invoice.issuerTaxId -> "ISSUED"
            invoice.recipientTaxId -> "RECEIVED"
            else -> null
        }

    private fun Invoice.toJson() =
        buildJsonObject {
            put("id", id)
            put("status", status)
            put("type", type)
            put("version", version)
            put(
                "issuer",
                if (issuerName == null &&
                    issuerTaxId == null
                ) {
                    JsonNull
                } else {
                    buildJsonObject {
                        put("name", issuerName)
                        put("nif", issuerTaxId)
                    }
                },
            )
            put(
                "recipient",
                if (recipientName == null &&
                    recipientTaxId == null
                ) {
                    JsonNull
                } else {
                    buildJsonObject {
                        put("name", recipientName)
                        put("nif", recipientTaxId)
                    }
                },
            )
            put("number", number)
            put("issueDate", date?.toString())
            put("concept", concept)
            put("currency", "EUR")
            put("corrective", corrective)
            put(
                "vatLines",
                buildJsonArray {
                    lines.forEach { line ->
                        add(
                            buildJsonObject {
                                put("vatRate", money(line.rate))
                                put("base", money(line.base))
                                put("vatAmount", money(line.quota))
                                put("surcharge", money(line.surcharge))
                                put("noVatCause", line.cause)
                            },
                        )
                    }
                },
            )
            put("withholdings", money(withholding))
            put("total", total?.let { money(it) })
            put("quarterClosed", closed(date))
            put(
                "recognition",
                recognition?.let { result ->
                    buildJsonObject {
                        put("result", result)
                        put("uncertainFields", JsonArray(uncertainFields.map { JsonPrimitive(it) }))
                    }
                }
                    ?: JsonNull,
            )
            put(
                "notices",
                JsonArray(
                    notices(this@toJson).map { (field, blocking, code) ->
                        buildJsonObject {
                            put("field", field)
                            put("code", code)
                            put("blocking", blocking)
                            put("message", noticeMessage(code))
                        }
                    },
                ),
            )
        }

    private fun recognitionEntry(result: String) =
        buildJsonObject {
            put("result", result)
            put("model", "demo")
            put("createdAt", state.now().toString())
            put("proposal", JsonNull)
        }

    private fun change(
        action: String,
        request: DemoRequest,
        before: JsonObject,
    ) = buildJsonObject {
        put("action", action)
        put("authorId", request.caller?.employeeId)
        put("occurredAt", state.now().toString())
        put("previousValues", JsonObject(before.filterKeys { it in setOf("number", "issueDate", "total", "withholdings", "type") }))
    }

    // --- Quarters ---

    private fun quarterOf(date: LocalDate) = date.year to (date.month.number - 1) / 3 + 1

    private fun closed(date: LocalDate?) = date != null && quarters[quarterOf(date)] == true

    private fun event(
        action: String,
        reason: String?,
        at: Instant = state.now(),
    ) = buildJsonObject {
        put("action", action)
        put("authorId", DemoState.ADMIN_ID)
        put("occurredAt", at.toString())
        put("reason", reason)
    }

    private fun quarterJson(
        year: Int,
        quarter: Int,
    ) = buildJsonObject {
        put("quarter", quarter)
        put("closed", quarters[year to quarter] == true)
        put("events", JsonArray(quarterEvents[year to quarter].orEmpty()))
    }

    private fun quarters(
        request: DemoRequest,
        s: List<String>,
    ): DemoReply? {
        if (s.isEmpty()) {
            val year = request.query["year"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDATION", "Falta el año")
            return json(JsonArray((1..4).map { quarterJson(year, it) }))
        }
        val year = s[0].toIntOrNull() ?: return null
        val quarter =
            s.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..4 }
                ?: return error(HttpStatusCode.UnprocessableEntity, "INVALID_PERIOD", "Trimestre no válido")
        val key = year to quarter
        return when (s.getOrNull(2)) {
            "close" -> {
                if (quarters[key] == true) return conflict("QUARTER_CLOSED")
                val pending =
                    invoices.count {
                        (it.status == "DRAFT" || it.status == "PENDING_RECOGNITION") &&
                            it.date?.let(::quarterOf) == key
                    }
                if (pending >
                    0
                ) {
                    return error(
                        HttpStatusCode.Conflict,
                        "QUARTER_HAS_PENDING",
                        "Quedan $pending facturas sin confirmar en el trimestre",
                    )
                }
                quarters[key] = true
                quarterEvents.getOrPut(key) { mutableListOf() } += event("CLOSE", null)
                json(quarterJson(year, quarter))
            }
            "reopen" -> {
                if (quarters[key] != true) return conflict("QUARTER_OPEN")
                val reason =
                    request.body
                        .str("reason")
                        ?.trim()
                        .orEmpty()
                if (reason.length !in 10..500) return error(HttpStatusCode.BadRequest, "VALIDATION", "El motivo debe tener entre 10 y 500 caracteres")
                quarters[key] = false
                quarterEvents.getOrPut(key) { mutableListOf() } += event("REOPEN", reason)
                json(quarterJson(year, quarter))
            }
            else -> null
        }
    }

    // --- Reports ---

    private fun report(request: DemoRequest): DemoReply {
        val period = request.query["period"]?.uppercase()
        val year = request.query["year"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDATION", "Falta el año")
        val (from, to, label) =
            when (period) {
                "MONTHLY" -> {
                    val month = request.query["month"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDATION", "Falta el mes")
                    val start = LocalDate(year, month, 1)
                    Triple(start, start.plusMonths(1).minus(DatePeriod(days = 1)), "$year-${month.toString().padStart(2, '0')}")
                }
                "QUARTERLY" -> {
                    val quarter =
                        request.query["quarter"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDATION", "Falta el trimestre")
                    val start = LocalDate(year, (quarter - 1) * 3 + 1, 1)
                    Triple(start, start.plusMonths(3).minus(DatePeriod(days = 1)), "$year-T$quarter")
                }
                "YEARLY" -> Triple(LocalDate(year, 1, 1), LocalDate(year, 12, 31), "$year")
                else -> return error(HttpStatusCode.UnprocessableEntity, "INVALID_PERIOD", "Periodo no válido")
            }
        val inPeriod = invoices.filter { it.date != null && it.date!! in from..to }
        val confirmed = inPeriod.filter { it.status == "CONFIRMED" }

        fun group(type: String): JsonObject {
            val list = confirmed.filter { it.type == type }
            val sign = { invoice: Invoice -> if (invoice.corrective) -1 else 1 }
            val lines = list.flatMap { invoice -> invoice.lines.map { sign(invoice) to it } }
            return buildJsonObject {
                put("invoices", list.size)
                put("base", money(lines.sumOf { (s, l) -> s * l.base }))
                put(
                    "vatByRate",
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
                put("surcharge", money(lines.sumOf { (s, l) -> s * l.surcharge }))
                put("withholdings", money(list.sumOf { sign(it) * it.withholding }))
                put("total", money(list.sumOf { sign(it) * (it.total ?: 0) }))
                put(
                    "noVatBase",
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
        val issued = group("ISSUED")
        val received = group("RECEIVED")
        val pending = inPeriod.count { it.status == "DRAFT" || it.status == "PENDING_RECOGNITION" }
        return when (request.query["format"]?.lowercase() ?: "json") {
            "csv" ->
                DemoReply.Bytes(
                    (
                        "Tipo;Facturas;Base;Retenciones;Total\r\n" +
                            "Emitidas;${issued.str(
                                "invoices",
                            )};${issued.str(
                                "base",
                            )?.replace(
                                '.',
                                ',',
                            )};${issued.str("withholdings")?.replace('.', ',')};${issued.str("total")?.replace('.', ',')}\r\n" +
                            "Recibidas;${received.str(
                                "invoices",
                            )};${received.str(
                                "base",
                            )?.replace(
                                '.',
                                ',',
                            )};${received.str("withholdings")?.replace('.', ',')};${received.str("total")?.replace('.', ',')}\r\n"
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
                                "invoices",
                            )} facturas, base ${issued.str("base")} EUR, total ${issued.str("total")} EUR",
                            "Recibidas: ${received.str(
                                "invoices",
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
                            "period",
                            buildJsonObject {
                                put("type", period)
                                put("year", year)
                                put("month", if (period == "MONTHLY") request.query["month"]?.toIntOrNull() else null)
                                put("quarter", if (period == "QUARTERLY") request.query["quarter"]?.toIntOrNull() else null)
                                put("from", from.toString())
                                put("to", to.toString())
                            },
                        )
                        put("issued", issued)
                        put("received", received)
                        put("pending", pending)
                        put(
                            "closedQuarters",
                            JsonArray(
                                quarters.filter { it.value && it.key.first == year }.keys.map { (y, q) ->
                                    buildJsonObject {
                                        put("year", y)
                                        put("quarter", q)
                                        put("from", quarterEvents[y to q]?.lastOrNull { it.str("action") == "CLOSE" }?.str("occurredAt"))
                                    }
                                },
                            ),
                        )
                        put("calculatedAt", state.now().toString())
                    },
                )
            else -> error(HttpStatusCode.UnprocessableEntity, "VALIDATION", "Formato no válido")
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
