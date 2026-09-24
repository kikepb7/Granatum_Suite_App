package com.granatum.feature.inventory.database

import androidx.room.RoomDatabaseConstructor

@Suppress("KotlinNoActualForExpect")
expect object AppInventoryDatabaseConstructor : RoomDatabaseConstructor<AppInventoryDatabase> {
    override fun initialize(): AppInventoryDatabase
}
