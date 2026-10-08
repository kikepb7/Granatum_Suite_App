package com.granatum.feature.clockin.domain.model

import kotlin.time.Instant

enum class ClockEventType {
    CLOCK_IN,
    CLOCK_OUT,
    BREAK_START,
    BREAK_END
}

/**
 * Sync state of a punch recorded on the device. Every punch is written locally first and sent
 * later by the sync engine (specs/005-fichaje-real, research D2).
 */
enum class SyncState {
    PENDING,
    SYNCING,
    SYNCED,

    /** Refused for good by the server; never retried (FR-009). */
    REJECTED
}

enum class ShiftStatus {
    CLOCKED_OUT,
    CLOCKED_IN,
    ON_BREAK
}

data class ClockEventModel(
    /** Client-generated UUID, also the `clientEventId` sent to the server. */
    val id: String,
    val type: ClockEventType,
    val clientTimestamp: Instant,
    val syncState: SyncState,
    val breakType: BreakType? = null,
    val rejection: ClockRejection? = null
)

/** The status after a sequence of punches, ignoring the ones the server refused. */
fun List<TimelineItem>.currentShiftStatus(): ShiftStatus {
    val last = filter { it.syncState != SyncState.REJECTED }.maxByOrNull { it.at } ?: return ShiftStatus.CLOCKED_OUT
    return when (last.type) {
        ClockEventType.CLOCK_IN, ClockEventType.BREAK_END -> ShiftStatus.CLOCKED_IN
        ClockEventType.BREAK_START -> ShiftStatus.ON_BREAK
        ClockEventType.CLOCK_OUT -> ShiftStatus.CLOCKED_OUT
    }
}
