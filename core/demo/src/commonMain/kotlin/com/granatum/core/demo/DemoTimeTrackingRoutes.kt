package com.granatum.core.demo

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

/** `/api/shifts…` and `/api/corrections…`: punches with idempotent clientEventId, shifts, monthly summary, corrections. */
class DemoTimeTrackingRoutes(
    private val state: DemoState,
) {
    fun route(request: DemoRequest): DemoReply? {
        val caller = request.caller ?: return null
        return if (request.segments.first() == "corrections") routeCorrections(caller, request) else routeShifts(caller, request)
    }

    private fun routeShifts(
        caller: DemoUser,
        request: DemoRequest,
    ): DemoReply? {
        val s = request.segments
        return when {
            s.size == 2 && s[1] == "clock-in" && request.method == HttpMethod.Post -> clockIn(caller, request.body)
            s.size >= 3 && s[1] == "employee" -> {
                if (s[2] != caller.employeeId && !request.isManager && caller.role != "REPRESENTATIVE") return forbidden
                if (s.size == 3) {
                    shiftsOf(s[2], request)
                } else if (s.size == 4 && s[3] == "summary") {
                    summary(s[2], request)
                } else {
                    null
                }
            }
            s.size >= 3 -> {
                val shift = state.shifts.firstOrNull { it.id == s[1] } ?: return notFound("SHIFT_NOT_FOUND")
                when (s.drop(2).joinToString("/")) {
                    "break/start" ->
                        punch(request.body) { at ->
                            if (shift.clockOut != null) return@punch conflict("SHIFT_NOT_IN_PROGRESS")
                            if (shift.breaks.any { it.end == null }) return@punch conflict("BREAK_ALREADY_OPEN")
                            shift.breaks += DemoBreak(state.newId(), request.body.str("type") ?: "OTHER", at)
                            null
                        } ?: json(shift.toJson())
                    "break/end" ->
                        punch(request.body) { at ->
                            val open = shift.breaks.lastOrNull { it.end == null } ?: return@punch conflict("BREAK_NOT_OPEN")
                            open.end = at
                            null
                        } ?: json(shift.toJson())
                    "clock-out" ->
                        punch(request.body) { at ->
                            if (shift.clockOut != null) return@punch conflict("SHIFT_NOT_IN_PROGRESS")
                            if (shift.breaks.any { it.end == null }) return@punch conflict("BREAK_OPEN_ON_CLOSE")
                            shift.clockOut = at
                            null
                        } ?: json(shift.toJson())
                    "corrections" ->
                        if (request.method == HttpMethod.Get) {
                            json(JsonArray(state.corrections.filter { it.shiftId == shift.id }.map { it.toJson() }))
                        } else {
                            requestCorrection(caller, shift, request.body)
                        }
                    else -> null
                }
            }
            else -> null
        }
    }

    /** `/api/corrections` (managers): the queue, and approving or rejecting a request. */
    private fun routeCorrections(
        caller: DemoUser,
        request: DemoRequest,
    ): DemoReply? {
        if (!request.isManager) return forbidden
        val s = request.segments
        if (s.size == 1 && request.method == HttpMethod.Get) {
            val status = request.query["status"]
            return json(
                JsonArray(
                    state.corrections
                        .filter { status == null || it.status == status }
                        .sortedByDescending { it.createdAt }
                        .map { it.toJson() },
                ),
            )
        }
        if (s.size != 3 || request.method != HttpMethod.Post) return null
        val correction = state.corrections.firstOrNull { it.id == s[1] } ?: return notFound("REQUEST_NOT_FOUND")
        if (correction.status != "PENDING") return conflict("REQUEST_ALREADY_RESOLVED")
        if (correction.requesterId == caller.employeeId) {
            return error(HttpStatusCode.Forbidden, "OWN_RESOLUTION", "No puedes resolver tu propia solicitud")
        }
        when (s[2]) {
            "approve" -> {
                state.shifts.firstOrNull { it.id == correction.shiftId }?.let { apply(it, correction.proposedValues) }
                correction.status = "APPROVED"
            }
            "reject" -> {
                val reason =
                    request.body.str("resolutionReason")?.takeIf { it.isNotBlank() }
                        ?: return error(HttpStatusCode.BadRequest, "VALIDATION", "Indica el motivo del rechazo")
                correction.status = "REJECTED"
                correction.resolutionReason = reason
            }
            else -> return null
        }
        correction.resolvedAt = state.now()
        correction.resolvedById = caller.employeeId
        return json(correction.toJson())
    }

    /** What an approved correction leaves in the shift: the proposed times and breaks. */
    private fun apply(
        shift: DemoShift,
        values: JsonObject,
    ) {
        values.str("clockIn")?.let { shift.clockIn = Instant.parse(it) }
        values.str("clockOut")?.let { shift.clockOut = Instant.parse(it) }
        (values["breaks"] as? JsonArray)?.let { breaks ->
            shift.breaks.clear()
            breaks.forEach { element ->
                val item = element.jsonObject
                shift.breaks +=
                    DemoBreak(
                        state.newId(),
                        item.str("type") ?: "OTHER",
                        Instant.parse(item.str("start").orEmpty()),
                        item.str("end")?.let { Instant.parse(it) },
                    )
            }
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
        state.shifts.firstOrNull { it.employeeId == caller.employeeId && it.clockOut == null }?.let {
            return error(HttpStatusCode.Conflict, "SHIFT_ALREADY_IN_PROGRESS", it.id)
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
        val from = request.query["from"]?.let { LocalDate.parse(it) }
        val to = request.query["to"]?.let { LocalDate.parse(it) }
        val list =
            state.shifts
                .filter { it.employeeId == employeeId }
                .filter {
                    val day = it.clockIn.toLocalDateTime(state.madrid).date
                    (from == null || day >= from) && (to == null || day <= to)
                }.sortedByDescending { it.clockIn }
        return json(JsonArray(list.map { it.toJson() }))
    }

    private fun summary(
        employeeId: String,
        request: DemoRequest,
    ): DemoReply {
        val year = request.query["year"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDATION", "Falta el año")
        val month = request.query["month"]?.toIntOrNull() ?: return error(HttpStatusCode.BadRequest, "VALIDATION", "Falta el mes")
        val corrected =
            state.corrections
                .filter { it.status == "APPROVED" }
                .map { it.shiftId }
                .toSet()
        val days =
            state.shifts
                .filter { it.employeeId == employeeId && it.clockOut != null }
                .filter {
                    it.clockIn
                        .toLocalDateTime(state.madrid)
                        .date
                        .let { d -> d.year == year && d.month.number == month }
                }.sortedBy { it.clockIn }
                .map { shift ->
                    buildJsonObject {
                        put(
                            "date",
                            shift.clockIn
                                .toLocalDateTime(state.madrid)
                                .date
                                .toString(),
                        )
                        put("clockIn", shift.clockIn.toString())
                        put("clockOut", shift.clockOut.toString())
                        put("workedMinutes", shift.workedMinutes)
                        put("breakMinutes", shift.breaks.sumOf { ((it.end ?: shift.clockOut!!) - it.start).inWholeMinutes }.toInt())
                        put("reconstructed", false)
                        put("corrected", shift.id in corrected)
                    }
                }
        return json(
            buildJsonObject {
                put("employeeId", employeeId)
                put("year", year)
                put("month", month)
                put("contractType", state.employee(employeeId)?.contractType ?: "FULL_TIME")
                put("totalWorkedMinutes", days.sumOf { it.int("workedMinutes") ?: 0 })
                put("days", JsonArray(days))
            },
        )
    }

    private fun requestCorrection(
        caller: DemoUser,
        shift: DemoShift,
        body: JsonObject,
    ): DemoReply {
        if (shift.clockOut == null) return error(HttpStatusCode.Conflict, "SHIFT_NOT_FINISHED", "El fichaje sigue abierto")
        val proposed =
            body["proposedValues"] as? JsonObject
                ?: return error(HttpStatusCode.BadRequest, "VALIDATION", "Faltan los valores propuestos")
        val correction =
            DemoCorrection(state.newId(), shift.id, caller.employeeId, body.str("reason").orEmpty(), proposed, state.now())
        state.corrections += correction
        return json(correction.toJson(), HttpStatusCode.Created)
    }

    private fun DemoCorrection.toJson() =
        buildJsonObject {
            put("id", id)
            put("shiftId", shiftId)
            put("requesterId", requesterId)
            put("status", status)
            put("reason", reason)
            put("proposedValues", proposedValues)
            resolutionReason?.let { put("resolutionReason", it) }
            resolvedAt?.let { put("resolvedAt", it.toString()) }
            resolvedById?.let { put("resolvedById", it) }
            put("createdAt", createdAt.toString())
        }

    private fun DemoShift.toJson() =
        buildJsonObject {
            put("id", id)
            put("employeeId", employeeId)
            put("clockIn", clockIn.toString())
            clockOut?.let { put("clockOut", it.toString()) }
            put("status", status)
            put("wasIncomplete", incomplete)
            workedMinutes?.let { put("workedMinutes", it) }
            put(
                "breaks",
                buildJsonArray {
                    breaks.forEach { pause ->
                        add(
                            buildJsonObject {
                                put("id", pause.id)
                                put("type", pause.type)
                                put("start", pause.start.toString())
                                pause.end?.let { put("end", it.toString()) }
                            },
                        )
                    }
                },
            )
        }
}
