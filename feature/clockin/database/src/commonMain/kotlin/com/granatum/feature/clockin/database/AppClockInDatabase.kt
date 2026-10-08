package com.granatum.feature.clockin.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.granatum.feature.clockin.database.dao.ClockEventDao
import com.granatum.feature.clockin.database.dao.ServerShiftDao
import com.granatum.feature.clockin.database.dao.ShiftDao
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import com.granatum.feature.clockin.database.entity.ServerShiftEntity
import com.granatum.feature.clockin.database.entity.ShiftEntity

@Database(
    entities = [
        ClockEventEntity::class,
        ShiftEntity::class,
        ServerShiftEntity::class
    ],
    version = 3,
    autoMigrations = [
        // 2: ClockEventEntity.employeeId (spec 004). A nullable column, so Room can derive it.
        AutoMigration(from = 1, to = 2)
        // 3: shifts (spec 005). Manual — see Migration2To3 — because existing punches have to be
        // grouped into shifts, which Room cannot derive.
    ]
)
@ConstructedBy(AppClockInDatabaseConstructor::class)
abstract class AppClockInDatabase : RoomDatabase() {
    abstract val clockEventDao: ClockEventDao
    abstract val shiftDao: ShiftDao
    abstract val serverShiftDao: ServerShiftDao

    companion object {
        const val DB_NAME = "app_clockin.db"
    }
}
