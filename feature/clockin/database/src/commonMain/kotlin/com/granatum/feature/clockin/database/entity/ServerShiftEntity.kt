package com.granatum.feature.clockin.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The last copy downloaded of a shift as the server records it, so it can be read without
 * coverage (specs/005-fichaje-real, research D6). Columns renamed to English in version 4.
 */
@Entity(tableName = "server_shift")
data class ServerShiftEntity(
    @PrimaryKey val id: String,
    val employeeId: String,
    val clockInEpochMillis: Long,
    val clockOutEpochMillis: Long?,
    /** IN_PROGRESS, CLOSED or INCOMPLETE. */
    val status: String,
    val workedMinutes: Int?,
    val wasIncomplete: Boolean,
    /** The breaks, as JSON: id, type, start, end. */
    val breaksJson: String,
    val corrected: Boolean = false,
    val reconstructed: Boolean = false,
    val fetchedAtEpochMillis: Long
)
