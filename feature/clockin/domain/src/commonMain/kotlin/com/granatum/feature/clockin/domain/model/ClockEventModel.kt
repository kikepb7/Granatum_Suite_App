package com.granatum.feature.clockin.domain.model

import kotlinx.datetime.Instant

enum class ClockEventType {
    CLOCK_IN,
    CLOCK_OUT,
    BREAK_START,
    BREAK_END
}

/**
 * Sync state of a single locally-recorded punch. Every event is written to
 * Room immediately (optimistic, offline-first) and only later pushed to the
 * backend by the sync engine — see `ClockEventSyncManager` in the data module.
 */
enum class SyncState {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

enum class ShiftStatus {
    CLOCKED_OUT,
    CLOCKED_IN,
    ON_BREAK
}

data class ClockEventModel(
    /** Client-generated UUID, also used as the idempotency key when syncing. */
    val id: String,
    val type: ClockEventType,
    val clientTimestamp: Instant,
    val serverTimestamp: Instant?,
    val syncState: SyncState
)

fun List<ClockEventModel>.currentShiftStatus(): ShiftStatus {
    val last = maxByOrNull { it.clientTimestamp } ?: return ShiftStatus.CLOCKED_OUT
    return when (last.type) {
        ClockEventType.CLOCK_IN, ClockEventType.BREAK_END -> ShiftStatus.CLOCKED_IN
        ClockEventType.BREAK_START -> ShiftStatus.ON_BREAK
        ClockEventType.CLOCK_OUT -> ShiftStatus.CLOCKED_OUT
    }
}
