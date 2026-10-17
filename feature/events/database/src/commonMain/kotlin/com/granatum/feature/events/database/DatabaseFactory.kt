package com.granatum.feature.events.database

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<AppEventsDatabase>
}
