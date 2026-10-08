package com.granatum.feature.clockin.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A shift as the device knows it. It exists before the server assigns an id, because the punches
 * that depend on the entry have to wait for that id (specs/005-fichaje-real, research D1).
 */
@Entity(tableName = "shift")
data class ShiftEntity(
    @PrimaryKey val localId: String,
    val employeeId: String,
    /** The server's shift id; null until the entry is registered or the shift is adopted. */
    val serverId: String?,
    val startedAtEpochMillis: Long
)
