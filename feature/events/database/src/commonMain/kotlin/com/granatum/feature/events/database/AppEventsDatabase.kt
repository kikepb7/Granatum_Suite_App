package com.granatum.feature.events.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.granatum.feature.events.database.dao.EventsDao
import com.granatum.feature.events.database.entity.EventEntity

@Database(entities = [EventEntity::class], version = 1)
@ConstructedBy(AppEventsDatabaseConstructor::class)
abstract class AppEventsDatabase : RoomDatabase() {
    abstract val eventsDao: EventsDao

    companion object {
        const val DB_NAME = "app_events.db"
    }
}
