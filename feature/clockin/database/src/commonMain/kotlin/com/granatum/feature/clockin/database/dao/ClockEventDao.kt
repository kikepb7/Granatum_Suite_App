package com.granatum.feature.clockin.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClockEventDao {

    @Query("SELECT * FROM clock_event WHERE employeeId = :employeeId ORDER BY clientTimestampEpochMillis ASC")
    fun observeAllEvents(employeeId: String): Flow<List<ClockEventEntity>>

    @Query(
        "SELECT * FROM clock_event WHERE employeeId = :employeeId " +
            "AND clientTimestampEpochMillis BETWEEN :fromEpochMillis AND :toEpochMillis " +
            "ORDER BY clientTimestampEpochMillis ASC"
    )
    fun observeEventsBetween(employeeId: String, fromEpochMillis: Long, toEpochMillis: Long): Flow<List<ClockEventEntity>>

    @Query("SELECT * FROM clock_event WHERE employeeId = :employeeId ORDER BY clientTimestampEpochMillis DESC LIMIT 1")
    suspend fun getLatestEvent(employeeId: String): ClockEventEntity?

    @Query("SELECT * FROM clock_event WHERE employeeId = :employeeId ORDER BY clientTimestampEpochMillis DESC LIMIT 1")
    fun observeLatestEvent(employeeId: String): Flow<ClockEventEntity?>

    @Query("SELECT COUNT(*) FROM clock_event WHERE employeeId = :employeeId AND syncState IN ('PENDING', 'FAILED')")
    fun observePendingCount(employeeId: String): Flow<Int>

    @Query(
        "SELECT * FROM clock_event WHERE employeeId = :employeeId AND syncState IN ('PENDING', 'FAILED') " +
            "ORDER BY clientTimestampEpochMillis ASC"
    )
    suspend fun getPendingEvents(employeeId: String): List<ClockEventEntity>

    @Upsert
    suspend fun upsertEvent(event: ClockEventEntity)

    @Query(
        "UPDATE clock_event SET syncState = 'SYNCED', serverTimestampEpochMillis = :serverTimestampEpochMillis " +
            "WHERE id = :id"
    )
    suspend fun markSynced(id: String, serverTimestampEpochMillis: Long)

    @Query(
        "UPDATE clock_event SET syncState = 'FAILED', retryCount = retryCount + 1, lastSyncAttemptEpochMillis = :attemptEpochMillis " +
            "WHERE id = :id"
    )
    suspend fun markFailed(id: String, attemptEpochMillis: Long)

    @Query("UPDATE clock_event SET syncState = 'SYNCING' WHERE id = :id")
    suspend fun markSyncing(id: String)
}
