package com.granatum.feature.clockin.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.granatum.feature.clockin.database.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Upsert
    suspend fun upsert(shift: ShiftEntity)

    @Query("SELECT * FROM shift WHERE localId = :localId")
    suspend fun get(localId: String): ShiftEntity?

    @Query("SELECT * FROM shift WHERE employeeId = :employeeId AND serverId = :serverId LIMIT 1")
    suspend fun getByServerId(employeeId: String, serverId: String): ShiftEntity?

    @Query("SELECT * FROM shift WHERE employeeId = :employeeId ORDER BY startedAtEpochMillis ASC")
    fun observeAll(employeeId: String): Flow<List<ShiftEntity>>

    @Query("UPDATE shift SET serverId = :serverId WHERE localId = :localId")
    suspend fun setServerId(localId: String, serverId: String)
}
