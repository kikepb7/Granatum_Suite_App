package com.granatum.feature.clockin.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** Whole-database operations for switching environment (specs/010-entornos, research D4). */
@Dao
interface ClockInMaintenanceDao {

    /** Punches of anyone on this device not yet accepted by the server. */
    @Query("SELECT COUNT(*) FROM clock_event WHERE syncState IN ('PENDING', 'SYNCING')")
    suspend fun countUnsynced(): Int

    @Query("DELETE FROM clock_event")
    suspend fun deleteEvents()

    @Query("DELETE FROM shift")
    suspend fun deleteShifts()

    @Query("DELETE FROM server_shift")
    suspend fun deleteServerShifts()

    @Transaction
    suspend fun clearAll() {
        deleteEvents()
        deleteShifts()
        deleteServerShifts()
    }
}
