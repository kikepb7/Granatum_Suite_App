package com.granatum.feature.clockin.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The last copy downloaded of a shift as the server records it, so it can be read without
 * coverage (specs/005-fichaje-real, research D6).
 */
@Entity(tableName = "server_shift")
data class ServerShiftEntity(
    @PrimaryKey val id: String,
    val employeeId: String,
    val entradaEpochMillis: Long,
    val salidaEpochMillis: Long?,
    /** EN_CURSO, CERRADO or INCOMPLETO. */
    val estado: String,
    val minutosTrabajados: Int?,
    val fueIncompleto: Boolean,
    /** The breaks, as JSON: id, tipo, inicio, fin. */
    val pausasJson: String,
    val corregido: Boolean = false,
    val reconstruido: Boolean = false,
    val fetchedAtEpochMillis: Long
)
