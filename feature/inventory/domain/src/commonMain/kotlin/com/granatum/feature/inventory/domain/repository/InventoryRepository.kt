package com.granatum.feature.inventory.domain.repository

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.ProductDraft
import com.granatum.feature.inventory.domain.model.ProductHistoryEntry
import com.granatum.feature.inventory.domain.model.ProductModel
import kotlinx.coroutines.flow.Flow

/**
 * Reads come from a local copy refreshed from the server and work offline; changes go straight
 * to the server and need a connection, because the inventory is shared and has no idempotency.
 */
interface InventoryRepository {
    fun observeProducts(): Flow<List<ProductModel>>
    fun observeProduct(id: String): Flow<ProductModel?>
    fun observeCategories(): Flow<List<CategoryModel>>
    fun observeHistory(productId: String): Flow<List<ProductHistoryEntry>>

    suspend fun refresh(): EmptyResult<InventoryError>
    suspend fun refreshProduct(id: String): EmptyResult<InventoryError>
    suspend fun refreshHistory(productId: String): EmptyResult<InventoryError>

    suspend fun create(draft: ProductDraft): Result<ProductModel, InventoryError>
    suspend fun update(id: String, draft: ProductDraft): Result<ProductModel, InventoryError>

    /** Sets the quantity, recorded in the product's history with [reason] (or a default one: the server requires one). */
    suspend fun setQuantity(id: String, quantity: Int, reason: String?): Result<ProductModel, InventoryError>
    suspend fun delete(id: String): EmptyResult<InventoryError>

    /** Uploads the product's image, replacing the previous one; the server checks type and size. */
    suspend fun setImage(id: String, bytes: ByteArray, fileName: String, mimeType: String): Result<ProductModel, InventoryError>

    /** Removes the product's image; nothing happens if it has none. */
    suspend fun removeImage(id: String): Result<ProductModel, InventoryError>

    suspend fun createCategory(name: String, description: String?): Result<CategoryModel, InventoryError>
    suspend fun updateCategory(id: String, name: String, description: String?): Result<CategoryModel, InventoryError>
    suspend fun deleteCategory(id: String): EmptyResult<InventoryError>
}
