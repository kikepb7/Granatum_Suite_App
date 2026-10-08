package com.granatum.feature.inventory.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.granatum.feature.inventory.database.entity.CategoryEntity
import com.granatum.feature.inventory.database.entity.MaterialEntity
import com.granatum.feature.inventory.database.entity.MaterialHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM material")
    fun observeMaterials(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM material WHERE id = :id")
    fun observeMaterial(id: String): Flow<MaterialEntity?>

    @Query("SELECT * FROM category ORDER BY name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM material_history WHERE materialId = :materialId ORDER BY atEpochMillis DESC")
    fun observeHistory(materialId: String): Flow<List<MaterialHistoryEntity>>

    @Upsert
    suspend fun upsertMaterial(material: MaterialEntity)

    @Upsert
    suspend fun upsertCategory(category: CategoryEntity)

    @Query("DELETE FROM material WHERE id = :id")
    suspend fun deleteMaterial(id: String)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteCategory(id: String)

    @Query("DELETE FROM material")
    suspend fun clearMaterials()

    @Query("DELETE FROM category")
    suspend fun clearCategories()

    @Query("DELETE FROM material_history WHERE materialId = :materialId")
    suspend fun clearHistory(materialId: String)

    @Upsert
    suspend fun upsertMaterials(materials: List<MaterialEntity>)

    @Upsert
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Upsert
    suspend fun upsertHistory(entries: List<MaterialHistoryEntity>)

    /** A full download replaces the cache: whatever is gone from the server goes here too. */
    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>, materials: List<MaterialEntity>) {
        clearMaterials()
        clearCategories()
        upsertCategories(categories)
        upsertMaterials(materials)
    }

    @Transaction
    suspend fun replaceHistory(materialId: String, entries: List<MaterialHistoryEntity>) {
        clearHistory(materialId)
        upsertHistory(entries)
    }
}
