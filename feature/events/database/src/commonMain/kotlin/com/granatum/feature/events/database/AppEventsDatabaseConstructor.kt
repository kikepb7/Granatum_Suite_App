package com.granatum.feature.events.database

import androidx.room.RoomDatabaseConstructor

@Suppress("KotlinNoActualForExpect")
expect object AppEventsDatabaseConstructor : RoomDatabaseConstructor<AppEventsDatabase> {
    override fun initialize(): AppEventsDatabase
}
