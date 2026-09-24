package com.granatum.feature.inventory.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.granatum.feature.inventory.database.entity.MaterialEntity
import com.granatum.feature.inventory.database.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {

    @Query("SELECT * FROM material ORDER BY name ASC")
    fun observeAllMaterials(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM material WHERE materialId = :materialId")
    fun observeMaterial(materialId: String): Flow<MaterialEntity?>

    @Query("SELECT * FROM stock_movement WHERE materialId = :materialId ORDER BY changedAtEpochMillis DESC")
    fun observeStockHistory(materialId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT materialId FROM material")
    suspend fun getAllMaterialIds(): List<String>

    @Upsert
    suspend fun upsertMaterial(material: MaterialEntity)

    @Upsert
    suspend fun upsertMaterials(materials: List<MaterialEntity>)

    @Upsert
    suspend fun upsertStockMovement(movement: StockMovementEntity)

    @Query("DELETE FROM material WHERE materialId = :materialId")
    suspend fun deleteMaterialById(materialId: String)

    @Transaction
    suspend fun syncMaterials(materials: List<MaterialEntity>) {
        val localIds = getAllMaterialIds()
        val serverIds = materials.map { it.materialId }.toSet()
        val staleIds = localIds.filter { it !in serverIds }
        upsertMaterials(materials)
        staleIds.forEach { deleteMaterialById(it) }
    }
}
