package com.granatum.feature.clockin.data.merge

import com.granatum.feature.clockin.data.mappers.breaks
import com.granatum.feature.clockin.data.mappers.toBreakType
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import com.granatum.feature.clockin.database.entity.ServerShiftEntity
import com.granatum.feature.clockin.database.entity.ShiftEntity
import com.granatum.feature.clockin.domain.model.ClockEventType
import com.granatum.feature.clockin.domain.model.ClockRejection
import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.model.ShiftState
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.model.SyncState
import com.granatum.feature.clockin.domain.model.TimelineItem
import com.granatum.feature.clockin.domain.model.currentShiftStatus
import kotlin.time.Instant

/**
 * What the server has recorded, with what is still on the device on top
 * (specs/005-fichaje-real, research D6, D7). Pure, so every combination is tested.
 *
 * - A server shift with a local shift pointing at it: the server's lines, plus the local
 *   punches not yet registered.
 * - A server shift alone (made on another device, or before this install): the server's lines.
 * - A local shift the server does not know yet: its punches as they stand.
 */
object ShiftMerger {

    fun merge(
        server: List<ServerShiftEntity>,
        local: List<ShiftEntity>,
        events: List<ClockEventEntity>,
        now: Instant
    ): List<ShiftModel> {
        val eventsByShift = events.groupBy { it.shiftLocalId }
        val serverById = server.associateBy { it.id }
        val claimed = mutableSetOf<String>()

        val fromLocal = local.map { shift ->
            val ownEvents = eventsByShift[shift.localId].orEmpty()
            val serverCopy = shift.serverId?.let(serverById::get)
            if (serverCopy != null) {
                claimed += serverCopy.id
                combine(serverCopy, ownEvents.filter { it.syncState != SYNCED }, now)
            } else {
                localOnly(shift, ownEvents, now)
            }
        }
        val serverOnly = server.filter { it.id !in claimed }.map { combine(it, emptyList(), now) }

        return (fromLocal + serverOnly)
            .filter { it.items.isNotEmpty() }
            .sortedBy { it.start }
    }

    private fun combine(server: ServerShiftEntity, unsent: List<ClockEventEntity>, now: Instant): ShiftModel {
        val items = (serverItems(server) + unsent.map { it.toItem(now) }).sortedBy { it.at }
        val hasUnsent = unsent.any { it.syncState != REJECTED }
        val state = if (hasUnsent) stateOf(items) else when (server.estado) {
            "EN_CURSO" -> ShiftState.IN_PROGRESS
            "INCOMPLETO" -> ShiftState.INCOMPLETE
            else -> ShiftState.CLOSED
        }
        val serverMinutes = server.minutosTrabajados
        return ShiftModel(
            key = server.id,
            serverId = server.id,
            start = Instant.fromEpochMilliseconds(server.entradaEpochMillis),
            end = items.lastOrNull { it.type == ClockEventType.CLOCK_OUT && it.syncState != SyncState.REJECTED }?.at,
            items = items,
            state = state,
            workedMinutes = if (!hasUnsent && serverMinutes != null) serverMinutes else estimateMinutes(items, now),
            isWorkedMinutesProvisional = hasUnsent || serverMinutes == null,
            incomplete = server.fueIncompleto || server.estado == "INCOMPLETO",
            corrected = server.corregido,
            reconstructed = server.reconstruido
        )
    }

    private fun localOnly(shift: ShiftEntity, events: List<ClockEventEntity>, now: Instant): ShiftModel {
        val items = events.map { it.toItem(now) }.sortedBy { it.at }
        return ShiftModel(
            key = shift.localId,
            serverId = shift.serverId,
            start = Instant.fromEpochMilliseconds(shift.startedAtEpochMillis),
            end = items.lastOrNull { it.type == ClockEventType.CLOCK_OUT && it.syncState != SyncState.REJECTED }?.at,
            items = items,
            state = stateOf(items),
            workedMinutes = estimateMinutes(items, now),
            isWorkedMinutesProvisional = true
        )
    }

    private fun serverItems(server: ServerShiftEntity): List<TimelineItem> = buildList {
        add(TimelineItem(ClockEventType.CLOCK_IN, Instant.fromEpochMilliseconds(server.entradaEpochMillis), SyncState.SYNCED))
        server.breaks().forEach { pausa ->
            add(TimelineItem(ClockEventType.BREAK_START, Instant.parse(pausa.inicio), SyncState.SYNCED, pausa.tipo.toBreakType()))
            pausa.fin?.let { add(TimelineItem(ClockEventType.BREAK_END, Instant.parse(it), SyncState.SYNCED, pausa.tipo.toBreakType())) }
        }
        server.salidaEpochMillis?.let {
            add(TimelineItem(ClockEventType.CLOCK_OUT, Instant.fromEpochMilliseconds(it), SyncState.SYNCED))
        }
    }

    private fun ClockEventEntity.toItem(now: Instant): TimelineItem {
        val at = Instant.fromEpochMilliseconds(clientTimestampEpochMillis)
        val state = runCatching { SyncState.valueOf(syncState) }.getOrDefault(SyncState.PENDING)
        return TimelineItem(
            type = ClockEventType.valueOf(type),
            at = at,
            syncState = state,
            breakType = breakType?.toBreakType(),
            rejection = rejectionCode?.takeIf { state == SyncState.REJECTED }?.let { ClockRejection.fromServerCode(it, at, now) },
            eventId = id
        )
    }

    private fun stateOf(items: List<TimelineItem>): ShiftState =
        if (items.currentShiftStatus() == ShiftStatus.CLOCKED_OUT) ShiftState.CLOSED else ShiftState.IN_PROGRESS

    /** Worked time from the lines that count; an open shift runs until now. */
    fun estimateMinutes(items: List<TimelineItem>, now: Instant): Int {
        var worked = 0L
        var workingSince: Instant? = null
        items.filter { it.syncState != SyncState.REJECTED }.sortedBy { it.at }.forEach { item ->
            when (item.type) {
                ClockEventType.CLOCK_IN, ClockEventType.BREAK_END -> if (workingSince == null) workingSince = item.at
                ClockEventType.BREAK_START, ClockEventType.CLOCK_OUT -> workingSince?.let {
                    worked += (item.at - it).inWholeMilliseconds
                    workingSince = null
                }
            }
        }
        workingSince?.let { worked += (now - it).inWholeMilliseconds.coerceAtLeast(0) }
        return (worked / 60_000L).toInt()
    }

    private const val SYNCED = "SYNCED"
    private const val REJECTED = "REJECTED"
}
