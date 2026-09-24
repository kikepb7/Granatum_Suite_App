package com.granatum.feature.inventory.database

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<AppInventoryDatabase>
}
