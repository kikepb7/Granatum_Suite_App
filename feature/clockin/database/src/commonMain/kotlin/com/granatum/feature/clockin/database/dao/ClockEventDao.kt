package com.granatum.feature.clockin.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import kotlinx.coroutines.flow.Flow

/** Every query is scoped to one person (spec 004, FR-028). */
@Dao
interface ClockEventDao {
    @Query("SELECT * FROM clock_event WHERE employeeId = :employeeId ORDER BY clientTimestampEpochMillis ASC")
    fun observeAllEvents(employeeId: String): Flow<List<ClockEventEntity>>

    @Query("SELECT COUNT(*) FROM clock_event WHERE employeeId = :employeeId AND syncState IN ('PENDING', 'SYNCING')")
    fun observePendingCount(employeeId: String): Flow<Int>

    @Query(
        "SELECT * FROM clock_event WHERE employeeId = :employeeId AND syncState IN ('PENDING', 'SYNCING') " +
            "ORDER BY clientTimestampEpochMillis ASC"
    )
    suspend fun getPendingEvents(employeeId: String): List<ClockEventEntity>

    @Query("SELECT * FROM clock_event WHERE shiftLocalId = :shiftLocalId ORDER BY clientTimestampEpochMillis ASC")
    suspend fun getEventsOfShift(shiftLocalId: String): List<ClockEventEntity>

    @Upsert
    suspend fun upsertEvent(event: ClockEventEntity)

    @Query(
        "UPDATE clock_event SET syncState = 'SYNCED', serverTimestampEpochMillis = :serverTimestampEpochMillis " +
            "WHERE id = :id"
    )
    suspend fun markSynced(id: String, serverTimestampEpochMillis: Long)

    @Query("UPDATE clock_event SET syncState = 'REJECTED', rejectionCode = :code WHERE id = :id")
    suspend fun markRejected(id: String, code: String)

    /** Every event of a shift still waiting, when its entry could not be registered. */
    @Query(
        "UPDATE clock_event SET syncState = 'REJECTED', rejectionCode = :code " +
            "WHERE shiftLocalId = :shiftLocalId AND syncState IN ('PENDING', 'SYNCING')"
    )
    suspend fun rejectPendingOfShift(shiftLocalId: String, code: String)

    @Query(
        "UPDATE clock_event SET syncState = 'PENDING', retryCount = retryCount + 1, " +
            "lastSyncAttemptEpochMillis = :attemptEpochMillis WHERE id = :id"
    )
    suspend fun markRetry(id: String, attemptEpochMillis: Long)

    @Query("UPDATE clock_event SET syncState = 'SYNCING' WHERE id = :id")
    suspend fun markSyncing(id: String)
}
