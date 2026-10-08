package com.granatum.feature.clockin.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.granatum.feature.clockin.database.entity.ServerShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerShiftDao {
    @Upsert
    suspend fun upsert(shift: ServerShiftEntity)

    @Upsert
    suspend fun upsertAll(shifts: List<ServerShiftEntity>)

    @Query("SELECT * FROM server_shift WHERE employeeId = :employeeId ORDER BY entradaEpochMillis ASC")
    fun observeAll(employeeId: String): Flow<List<ServerShiftEntity>>

    @Query(
        "DELETE FROM server_shift WHERE employeeId = :employeeId " +
            "AND entradaEpochMillis BETWEEN :fromEpochMillis AND :toEpochMillis"
    )
    suspend fun deleteRange(employeeId: String, fromEpochMillis: Long, toEpochMillis: Long)

    /** A downloaded range replaces what was cached for it: a shift gone from the server goes here too. */
    @Transaction
    suspend fun replaceRange(employeeId: String, fromEpochMillis: Long, toEpochMillis: Long, shifts: List<ServerShiftEntity>) {
        deleteRange(employeeId, fromEpochMillis, toEpochMillis)
        upsertAll(shifts)
    }
}
