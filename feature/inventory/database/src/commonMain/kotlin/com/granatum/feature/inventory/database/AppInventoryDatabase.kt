package com.granatum.feature.inventory.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.granatum.feature.inventory.database.dao.MaterialDao
import com.granatum.feature.inventory.database.entity.MaterialEntity
import com.granatum.feature.inventory.database.entity.StockMovementEntity

@Database(
    entities = [
        MaterialEntity::class,
        StockMovementEntity::class
    ],
    version = 1
)
@ConstructedBy(AppInventoryDatabaseConstructor::class)
abstract class AppInventoryDatabase : RoomDatabase() {
    abstract val materialDao: MaterialDao

    companion object {
        const val DB_NAME = "app_inventory.db"
    }
}
