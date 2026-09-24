package com.granatum.feature.clockin.database

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<AppClockInDatabase>
}
