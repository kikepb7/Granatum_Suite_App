package com.granatum.core.data.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

/** `/api/fichajes…`: punches with idempotent clientEventId, shifts, monthly summary, corrections. */
class DemoTimeTrackingRoutes(
    private val state: DemoState,
) {
    fun route(request: DemoRequest): DemoReply? {
        val s = request.segments
        val caller = request.caller ?: return null
        return when {
            s.size == 2 && s[1] == "entrada" && request.method == HttpMethod.Post -> clockIn(caller, request.body)
            s.size >= 3 && s[1] == "empleado" -> {
                if (s[2] != caller.employeeId && !request.isManager && caller.role != "REPRESENTANTE") return forbidden
                if (s.size == 3) {
                    shiftsOf(s[2], request)
                } else if (s.getOrNull(3) == "resumen") {
                    summary(s[2], request)
                } else {
                    null
                }
            }
            s.size >= 3 -> {
                val shift = state.shifts.firstOrNull { it.id == s[1] } ?: return notFound("FICHAJE_NOT_FOUND")
                when (s.drop(2).joinToString("/")) {
                    "pausa/inicio" ->
                        punch(request.body) { at ->
                            if (shift.pauses.any { it.end == null }) return@punch conflict("PAUSA_YA_INICIADA")
                            shift.pauses += DemoPause(state.newId(), request.body.str("tipo") ?: "OTRO", at)
                            null
                        } ?: json(shift.toJson())
                    "pausa/fin" ->
                        punch(request.body) { at ->
                            val open = shift.pauses.lastOrNull { it.end == null } ?: return@punch conflict("PAUSA_NO_INICIADA")
                            open.end = at
                            null
                        } ?: json(shift.toJson())
                    "salida" ->
                        punch(request.body) { at ->
                            if (shift.pauses.any { it.end == null }) return@punch conflict("PAUSA_EN_CURSO")
                            shift.exit = at
                            null
                        } ?: json(shift.toJson())
                    "correcciones" ->
                        if (request.method ==
                            HttpMethod.Get
                        ) {
                            corrections(shift.id)
                        } else {
                            requestCorrection(caller, shift, request.body)
                        }
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun conflict(code: String) = error(HttpStatusCode.Conflict, code, code)

    /** Runs [apply] once per clientEventId; a resent punch just gets the shift back. */
    private fun punch(
        body: JsonObject,
        apply: (Instant) -> DemoReply?,
    ): DemoReply? {
        val eventId = body.str("clientEventId").orEmpty()
        if (eventId in state.processedEvents) return null
        val at = body.str("occurredAt")?.let { Instant.parse(it) } ?: state.now()
        return apply(at).also { if (it == null) state.processedEvents[eventId] = eventId }
    }

    private fun clockIn(
        caller: DemoUser,
        body: JsonObject,
    ): DemoReply {
        val eventId = body.str("clientEventId").orEmpty()
        state.processedEvents[eventId]?.let { id ->
            state.shifts.firstOrNull { it.id == id }?.let { return json(it.toJson(), HttpStatusCode.Created) }
        }
        state.shifts.firstOrNull { it.employeeId == caller.employeeId && it.exit == null }?.let {
            return error(HttpStatusCode.Conflict, "FICHAJE_YA_EN_CURSO", it.id)
        }
        val shift = DemoShift(state.newId(), caller.employeeId, body.str("occurredAt")?.let { Instant.parse(it) } ?: state.now())
        state.shifts += shift
        state.processedEvents[eventId] = shift.id
        return json(shift.toJson(), HttpStatusCode.Created)
    }

    private fun shiftsOf(
        employeeId: String,
        request: DemoRequest,
    ): DemoReply {
        val from = request.query["desde"]?.let { LocalDate.parse(it) }
        val to = request.query["hasta"]?.let { LocalDate.parse(it) }
        val list =
            state.shifts
                .filter { it.employeeId == employeeId }
                .filter {
                    val day = it.entry.toLocalDateTime(state.madrid).date
                    (from == null || day >= from) && (to == null || day <= to)
                }.sortedByDescending { it.entry }
        return json(JsonArray(list.map { it.toJson() }))
    }

    private fun summary(
        employeeId: String,
        request: DemoRequest,
    ): DemoReply {
        val year = request.query["anio"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDACION", "anio")
        val month = request.query["mes"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDACION", "mes")
        val corrected =
            state.corrections
                .filter { it.reason.startsWith(APPROVED) }
                .map { it.shiftId }
                .toSet()
        val days =
            state.shifts
                .filter { it.employeeId == employeeId && it.exit != null }
                .filter {
                    it.entry
                        .toLocalDateTime(state.madrid)
                        .date
                        .let { d -> d.year == year && d.month.number == month }
                }.sortedBy { it.entry }
                .map { shift ->
                    buildJsonObject {
                        put(
                            "fecha",
                            shift.entry
                                .toLocalDateTime(state.madrid)
                                .date
                                .toString(),
                        )
                        put("entrada", shift.entry.toString())
                        put("salida", shift.exit.toString())
                        put("minutosTrabajados", shift.workedMinutes)
                        put("minutosPausa", shift.pauses.sumOf { ((it.end ?: shift.exit!!) - it.start).inWholeMinutes }.toInt())
                        put("reconstruido", false)
                        put("corregido", shift.id in corrected)
                    }
                }
        return json(
            buildJsonObject {
                put("empleadoId", employeeId)
                put("anio", year)
                put("mes", month)
                put("tipoContrato", state.employee(employeeId)?.contract ?: "JORNADA_COMPLETA")
                put("totalMinutosTrabajados", days.sumOf { it.int("minutosTrabajados") ?: 0 })
                put("dias", JsonArray(days))
            },
        )
    }

    private fun corrections(shiftId: String) = json(JsonArray(state.corrections.filter { it.shiftId == shiftId }.map { it.toJson() }))

    private fun requestCorrection(
        caller: DemoUser,
        shift: DemoShift,
        body: JsonObject,
    ): DemoReply {
        if (shift.exit == null) return error(HttpStatusCode.Conflict, "FICHAJE_NO_FINALIZADO", "El fichaje sigue abierto")
        val proposed =
            body["valoresPropuestos"] as? JsonObject ?: return error(HttpStatusCode.BadRequest, "VALIDACION", "valoresPropuestos")
        val correction = DemoCorrection(state.newId(), shift.id, caller.employeeId, body.str("motivo").orEmpty(), proposed, state.now())
        state.corrections += correction
        return json(correction.toJson(), HttpStatusCode.Created)
    }

    private fun DemoCorrection.toJson() =
        buildJsonObject {
            put("id", id)
            put("fichajeId", shiftId)
            put("solicitanteId", requesterId)
            put("estado", "PENDIENTE")
            put("motivo", reason)
            put("valoresPropuestos", proposed)
            put("creadaEn", createdAt.toString())
        }

    private fun DemoShift.toJson() =
        buildJsonObject {
            put("id", id)
            put("empleadoId", employeeId)
            put("entrada", entry.toString())
            exit?.let { put("salida", it.toString()) }
            put("estado", state)
            put("fueIncompleto", incomplete)
            workedMinutes?.let { put("minutosTrabajados", it) }
            put(
                "pausas",
                buildJsonArray {
                    pauses.forEach { pause ->
                        add(
                            buildJsonObject {
                                put("id", pause.id)
                                put("tipo", pause.type)
                                put("inicio", pause.start.toString())
                                pause.end?.let { put("fin", it.toString()) }
                            },
                        )
                    }
                },
            )
        }

    private companion object {
        const val APPROVED = "APROBADA:"
    }
}
