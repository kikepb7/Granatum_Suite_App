package com.granatum.feature.inventory.domain.repository

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialHistoryEntry
import com.granatum.feature.inventory.domain.model.MaterialModel
import kotlinx.coroutines.flow.Flow

/**
 * Reads come from a local copy refreshed from the server and work offline; changes go straight
 * to the server and need a connection, because the inventory is shared and has no idempotency
 * (specs/006-inventario-real, research D1).
 */
interface InventoryRepository {
    fun observeMaterials(): Flow<List<MaterialModel>>
    fun observeMaterial(id: String): Flow<MaterialModel?>
    fun observeCategories(): Flow<List<CategoryModel>>
    fun observeHistory(materialId: String): Flow<List<MaterialHistoryEntry>>

    suspend fun refresh(): EmptyResult<InventoryError>
    suspend fun refreshMaterial(id: String): EmptyResult<InventoryError>
    suspend fun refreshHistory(materialId: String): EmptyResult<InventoryError>

    suspend fun create(draft: MaterialDraft): Result<MaterialModel, InventoryError>
    suspend fun update(id: String, draft: MaterialDraft): Result<MaterialModel, InventoryError>
    suspend fun adjustQuantity(id: String, available: Int, reason: String): Result<MaterialModel, InventoryError>
    suspend fun delete(id: String): EmptyResult<InventoryError>

    suspend fun createCategory(name: String, description: String?): Result<CategoryModel, InventoryError>
    suspend fun updateCategory(id: String, name: String, description: String?): Result<CategoryModel, InventoryError>
    suspend fun deleteCategory(id: String): EmptyResult<InventoryError>
}
