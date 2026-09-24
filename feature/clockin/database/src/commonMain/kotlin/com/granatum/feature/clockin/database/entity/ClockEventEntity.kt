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
    val lastSyncAttemptEpochMillis: Long?
)
