package com.granatum.feature.clockin.database

import androidx.room.RoomDatabaseConstructor

@Suppress("KotlinNoActualForExpect")
expect object AppClockInDatabaseConstructor : RoomDatabaseConstructor<AppClockInDatabase> {
    override fun initialize(): AppClockInDatabase
}
