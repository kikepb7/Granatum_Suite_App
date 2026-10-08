package com.granatum.feature.inventory.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.granatum.feature.inventory.database.dao.InventoryDao
import com.granatum.feature.inventory.database.entity.CategoryEntity
import com.granatum.feature.inventory.database.entity.MaterialEntity
import com.granatum.feature.inventory.database.entity.MaterialHistoryEntity

/**
 * Version 2 (spec 006) replaces the version 1 model outright. Version 1 was a read cache that
 * never received real data, so it is dropped rather than migrated (research D2).
 */
@Database(
    entities = [
        CategoryEntity::class,
        MaterialEntity::class,
        MaterialHistoryEntity::class
    ],
    version = 2
)
@ConstructedBy(AppInventoryDatabaseConstructor::class)
abstract class AppInventoryDatabase : RoomDatabase() {
    abstract val inventoryDao: InventoryDao

    companion object {
        const val DB_NAME = "app_inventory.db"
    }
}
