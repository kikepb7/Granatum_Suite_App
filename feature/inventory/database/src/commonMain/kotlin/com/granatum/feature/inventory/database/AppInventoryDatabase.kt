package com.granatum.feature.inventory.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.granatum.feature.inventory.database.dao.InventoryDao
import com.granatum.feature.inventory.database.dao.InventoryMaintenanceDao
import com.granatum.feature.inventory.database.entity.CategoryEntity
import com.granatum.feature.inventory.database.entity.MaterialEntity
import com.granatum.feature.inventory.database.entity.MaterialHistoryEntity

/**
 * A read cache, so each new model replaces the previous one instead of migrating it: version 2
 * (spec 006) replaced version 1, and version 3 (spec 011, the backend's 010 material) replaces 2.
 * Nothing in it is waiting to be sent; it is downloaded again on the next refresh.
 */
@Database(
    entities = [
        CategoryEntity::class,
        MaterialEntity::class,
        MaterialHistoryEntity::class
    ],
    version = 3
)
@ConstructedBy(AppInventoryDatabaseConstructor::class)
abstract class AppInventoryDatabase : RoomDatabase() {
    abstract val inventoryDao: InventoryDao
    abstract val maintenanceDao: InventoryMaintenanceDao

    companion object {
        const val DB_NAME = "app_inventory.db"
    }
}
