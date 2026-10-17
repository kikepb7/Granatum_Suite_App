package com.granatum.core.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

/**
 * `/api/eventos` (backend feature 011): events take units out of the demo inventory while they
 * are scheduled and put them back when they finish or are deleted. Read by everyone; written by
 * everyone but REPRESENTANTE, as on the server. Events of past days are finished on the next
 * request, standing in for the server's nightly job.
 */
class DemoEventsRoutes(
    private val state: DemoState,
    private val inventory: DemoInventoryRoutes,
) {
    private class Line(
        val materialId: String,
        var quantity: Int,
        val description: String,
        val category: String,
    )

    private class Event(
        val id: String,
        var name: String,
        var description: String?,
        var date: LocalDate,
        var finished: Boolean,
        val createdBy: String,
        var finishedAt: Instant?,
        val createdAt: Instant,
        var updatedAt: Instant,
        val lines: MutableList<Line> = mutableListOf(),
    )

    private val events = mutableListOf<Event>()

    init {
        inventory.materialInUse = { id -> events.any { !it.finished && it.lines.any { line -> line.materialId == id } } }
        val today = state.today()
        val wedding = seed("Evento 1", "Boda de Juan y María", today.plus(DatePeriod(days = 9)))
        val available = inventory.availableJson().map { it as JsonObject }
        available.firstOrNull { it.str("descripcion")?.startsWith("Jarrón cilíndrico") == true }?.let { take(wedding, it, 4) }
        available.firstOrNull { it.str("descripcion")?.startsWith("Cinta") == true }?.let { take(wedding, it, 10) }
        seed("Evento 2", "Comunión de Lucía", today.plus(DatePeriod(days = 23)))
        val past = seed("Evento 0", "Bautizo de Mateo", today.plus(DatePeriod(days = -12)))
        past.finished = true
        past.finishedAt = state.now()
    }

    private fun seed(name: String, description: String, date: LocalDate): Event {
        val t = state.now()
        return Event(state.newId(), name, description, date, false, DemoState.MANAGER_ID, null, t, t).also { events += it }
    }

    private fun take(event: Event, material: JsonObject, units: Int) {
        val id = material.str("id")!!
        inventory.withdraw(id, units, event.createdBy, "Reservado para el evento «${event.name}»")
        event.lines += Line(id, units, material.str("descripcion")!!, material.str("categoria")!!)
    }

    fun route(request: DemoRequest): DemoReply? {
        finishPast()
        if (request.method != HttpMethod.Get && request.caller?.role == "REPRESENTANTE") return forbidden
        val s = request.segments
        val userId = request.caller!!.employeeId
        if (s.size == 2 && s[1] == "materiales" && request.method == HttpMethod.Get) return json(inventory.availableJson())
        if (s.size == 1) {
            return when (request.method) {
                HttpMethod.Get -> {
                    val wanted = request.query["estado"]
                    val shown = events.filter { wanted == null || it.status() == wanted }
                    json(JsonArray(shown.sortedWith(compareBy({ it.finished }, { it.date })).map { it.toJson() }))
                }
                HttpMethod.Post -> {
                    invalid(request.body)?.let { return it }
                    val t = state.now()
                    val event = Event(
                        state.newId(),
                        request.body.str("nombre")!!.trim(),
                        request.body.str("descripcion")?.trim()?.ifEmpty { null },
                        LocalDate.parse(request.body.str("fecha")!!),
                        false,
                        userId,
                        null,
                        t,
                        t,
                    )
                    events += event
                    json(event.toJson(), HttpStatusCode.Created)
                }
                else -> null
            }
        }
        val event = events.firstOrNull { it.id == s[1] } ?: return notFound("EVENTO_NOT_FOUND")
        return when {
            s.size == 2 && request.method == HttpMethod.Get -> json(event.toJson())
            s.size == 2 && request.method == HttpMethod.Put -> {
                if (event.finished) return finished()
                invalid(request.body)?.let { return it }
                event.name = request.body.str("nombre")!!.trim()
                event.description = request.body.str("descripcion")?.trim()?.ifEmpty { null }
                event.date = LocalDate.parse(request.body.str("fecha")!!)
                event.updatedAt = state.now()
                json(event.toJson())
            }
            s.size == 2 && request.method == HttpMethod.Delete -> {
                if (!event.finished) event.lines.forEach { inventory.putBack(it.materialId, it.quantity, userId, "Devuelto al borrar el evento «${event.name}»") }
                events -= event
                DemoReply.NoContent
            }
            s.size == 3 && s[2] == "finalizar" && request.method == HttpMethod.Post -> {
                finish(event, userId, automatic = false)
                json(event.toJson())
            }
            s.size == 4 && s[2] == "materiales" -> {
                if (event.finished) return finished()
                val materialId = s[3]
                val line = event.lines.firstOrNull { it.materialId == materialId }
                when (request.method) {
                    HttpMethod.Put -> {
                        val wanted = request.body.int("cantidad")
                        if (wanted == null || wanted < 1) return error(HttpStatusCode.BadRequest, "VALIDACION", "cantidad debe ser al menos 1")
                        val difference = wanted - (line?.quantity ?: 0)
                        if (difference > 0) {
                            val free = inventory.quantityOf(materialId) ?: return notFound("MATERIAL_NOT_FOUND")
                            if (free < difference) return stockError(free)
                            inventory.withdraw(materialId, difference, userId, "Reservado para el evento «${event.name}»")
                        } else if (difference < 0) {
                            inventory.putBack(materialId, -difference, userId, "Devuelto del evento «${event.name}»")
                        }
                        if (line == null) {
                            val summary = inventory.summaryJson(materialId)!!
                            event.lines += Line(materialId, wanted, summary.str("descripcion")!!, summary.str("categoria")!!)
                        } else {
                            line.quantity = wanted
                        }
                        event.updatedAt = state.now()
                        json(event.toJson())
                    }
                    HttpMethod.Delete -> {
                        if (line != null) {
                            inventory.putBack(materialId, line.quantity, userId, "Devuelto del evento «${event.name}»")
                            event.lines -= line
                            event.updatedAt = state.now()
                        }
                        json(event.toJson())
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun finishPast() {
        val today = state.today()
        events.filter { !it.finished && it.date < today }.forEach { finish(it, it.createdBy, automatic = true) }
    }

    private fun finish(event: Event, userId: String, automatic: Boolean) {
        if (event.finished) return
        val reason = if (automatic) "Devuelto automáticamente al finalizar el evento «${event.name}»" else "Devuelto al finalizar el evento «${event.name}»"
        event.lines.forEach { inventory.putBack(it.materialId, it.quantity, userId, reason) }
        event.finished = true
        event.finishedAt = state.now()
        event.updatedAt = state.now()
    }

    private fun invalid(body: JsonObject): DemoReply? {
        val name = body.str("nombre")?.trim()
        val date = body.str("fecha")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return when {
            name.isNullOrEmpty() || name.length > 100 || (body.str("descripcion")?.length ?: 0) > 500 || date == null ->
                error(HttpStatusCode.BadRequest, "VALIDACION", "Datos del evento no válidos")
            date < state.today() -> error(HttpStatusCode.BadRequest, "FECHA_PASADA", "La fecha del evento no puede ser anterior a hoy")
            else -> null
        }
    }

    private fun finished() = error(HttpStatusCode.Conflict, "EVENTO_FINALIZADO", "El evento ya ha finalizado y no se puede modificar")

    private fun stockError(free: Int) =
        json(
            buildJsonObject {
                put("code", "STOCK_INSUFICIENTE")
                put("message", "Solo hay $free unidades disponibles de ese material")
                put("disponible", free)
            },
            HttpStatusCode.Conflict,
        )

    private fun Event.status() = if (finished) "FINALIZADO" else "PROGRAMADO"

    private fun Event.toJson() =
        buildJsonObject {
            put("id", id)
            put("nombre", name)
            put("descripcion", description)
            put("fecha", date.toString())
            put("estado", status())
            put(
                "materiales",
                JsonArray(
                    lines.map { line ->
                        val current = inventory.summaryJson(line.materialId)
                        buildJsonObject {
                            put("materialId", line.materialId)
                            put("categoria", line.category)
                            put("descripcion", line.description)
                            put("cantidad", line.quantity)
                            put("localizacion", current?.get("localizacion") ?: JsonNull)
                            put("imagenUrl", current?.get("imagenUrl") ?: JsonNull)
                        }
                    },
                ),
            )
            put("creadoPor", createdBy)
            put("finalizadoEn", finishedAt?.toString())
            put("fechaAlta", createdAt.toString())
            put("fechaUltimaModificacion", updatedAt.toString())
        }
}
