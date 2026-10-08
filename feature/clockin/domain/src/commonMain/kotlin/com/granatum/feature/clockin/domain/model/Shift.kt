package com.granatum.feature.clockin.domain.model

import kotlin.time.Instant

/** The break types of the backend's contract, under the same names. */
enum class BreakType { COMIDA, DESCANSO, OTRO }

enum class ShiftState { IN_PROGRESS, CLOSED, INCOMPLETE }

/**
 * One line of a shift as the person sees it: an entry, a break start or end, an exit. It is
 * built either from what the server has recorded (then [syncState] is SYNCED) or from a punch
 * still on the device (PENDING, SYNCING or REJECTED). [eventId] is set only for the latter.
 */
data class TimelineItem(
    val type: ClockEventType,
    val at: Instant,
    val syncState: SyncState,
    val breakType: BreakType? = null,
    val rejection: ClockRejection? = null,
    val eventId: String? = null
)

/**
 * A working day, as the server records it, with the punches not yet sent on top
 * (specs/005-fichaje-real, research D6). [key] is the server id once it exists, the local id
 * before that.
 */
data class ShiftModel(
    val key: String,
    val serverId: String?,
    val start: Instant,
    val end: Instant?,
    val items: List<TimelineItem>,
    val state: ShiftState,
    /** The server's figure when it has one; otherwise a local estimate. */
    val workedMinutes: Int?,
    val isWorkedMinutesProvisional: Boolean,
    val incomplete: Boolean = false,
    val corrected: Boolean = false,
    val reconstructed: Boolean = false
) {
    val hasPending: Boolean get() = items.any { it.syncState == SyncState.PENDING || it.syncState == SyncState.SYNCING }
    val rejections: List<TimelineItem> get() = items.filter { it.syncState == SyncState.REJECTED }

    /** Only closed shifts the server already knows can be corrected (FICHAJE_NO_FINALIZADO). */
    val canRequestCorrection: Boolean get() = serverId != null && state != ShiftState.IN_PROGRESS
}

/** What the clock-in screen needs. */
data class TodayState(
    val status: ShiftStatus = ShiftStatus.CLOCKED_OUT,
    /** Today's shifts, most recent last; the open one, if any, is the last. */
    val shifts: List<ShiftModel> = emptyList(),
    val pendingCount: Int = 0,
    val rejected: List<TimelineItem> = emptyList()
) {
    val hasClockSkew: Boolean get() = rejected.any { it.rejection == ClockRejection.ClockSkew }
    val currentShift: ShiftModel? get() = shifts.lastOrNull()?.takeIf { it.state == ShiftState.IN_PROGRESS }
}
