package com.granatum.feature.clockin.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.granatum.feature.clockin.database.dao.ClockEventDao
import com.granatum.feature.clockin.database.entity.ClockEventEntity

@Database(
    entities = [
        ClockEventEntity::class
    ],
    version = 1
)
@ConstructedBy(AppClockInDatabaseConstructor::class)
abstract class AppClockInDatabase : RoomDatabase() {
    abstract val clockEventDao: ClockEventDao

    companion object {
        const val DB_NAME = "app_clockin.db"
    }
}
