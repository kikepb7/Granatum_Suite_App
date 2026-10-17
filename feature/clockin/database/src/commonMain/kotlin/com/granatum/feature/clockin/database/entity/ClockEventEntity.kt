package com.granatum.feature.clockin.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One punch, and the outbound queue at the same time: written here first (offline-first) and
 * sent later by the sync engine. `id` is the `clientEventId` the server uses to make retries
 * idempotent, so it is generated once and never changes (specs/005-fichaje-real, research D5).
 */
@Entity(tableName = "clock_event")
data class ClockEventEntity(
    @PrimaryKey val id: String,
    val type: String,
    /** The `occurredAt` sent to the server. */
    val clientTimestampEpochMillis: Long,
    val serverTimestampEpochMillis: Long?,
    /** PENDING, SYNCING, SYNCED or REJECTED. */
    val syncState: String,
    val retryCount: Int,
    val lastSyncAttemptEpochMillis: Long?,
    /**
     * Who made the punch (spec 004, FR-028). Still nullable in the schema only because SQLite
     * cannot tighten a column in place: migration 2 -> 3 deletes every row without one.
     */
    val employeeId: String? = null,
    /** The local shift it belongs to (version 3). */
    val shiftLocalId: String? = null,
    /** MEAL, REST or OTHER; only on BREAK_START (version 3; English values since version 4). */
    val breakType: String? = null,
    /** The server's `code` when REJECTED, or SHIFT_NOT_REGISTERED (version 3; English since version 4). */
    val rejectionCode: String? = null
)
