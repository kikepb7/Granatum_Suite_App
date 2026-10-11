package com.granatum.feature.inventory.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** Whole-database operations for switching environment (specs/010-entornos, research D4). */
@Dao
interface InventoryMaintenanceDao {

    @Query("DELETE FROM material_history")
    suspend fun deleteHistory()

    @Query("DELETE FROM material")
    suspend fun deleteMaterials()

    @Query("DELETE FROM category")
    suspend fun deleteCategories()

    @Transaction
    suspend fun clearAll() {
        deleteHistory()
        deleteMaterials()
        deleteCategories()
    }
}
