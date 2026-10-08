package com.granatum.feature.clockin.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Doubles as the full punch log AND the outbound sync queue: every row is
 * written here first (offline-first), `syncState` tracks whether it has
 * reached the backend yet, and `id` is the client-generated idempotency key
 * sent with the sync request so a retried push never double-counts a punch
 * on the server.
 */
@Entity(tableName = "clock_event")
data class ClockEventEntity(
    @PrimaryKey val id: String,
    val type: String,
    val clientTimestampEpochMillis: Long,
    val serverTimestampEpochMillis: Long?,
    val syncState: String,
    val retryCount: Int,
    val lastSyncAttemptEpochMillis: Long?,
    /**
     * Who made the punch: the employee id of the session at the time (spec 004, FR-028). The
     * server attributes a punch to whoever signs the request, so a punch must never be sent, or
     * shown, under another person's session. Null only for rows written before sign-in existed;
     * those are never sent.
     */
    val employeeId: String? = null
)
