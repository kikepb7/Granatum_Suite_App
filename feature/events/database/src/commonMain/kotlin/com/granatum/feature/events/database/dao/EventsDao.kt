package com.granatum.feature.events.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.granatum.feature.events.database.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventsDao {

    @Query("SELECT * FROM event ORDER BY date")
    fun observeEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM event WHERE id = :id")
    fun observeEvent(id: String): Flow<EventEntity?>

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM event WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM event")
    suspend fun clear()

    /** The server's list replaces the cache, so an event deleted elsewhere disappears here. */
    @Transaction
    suspend fun replaceAll(events: List<EventEntity>) {
        clear()
        upsertAll(events)
    }
}
