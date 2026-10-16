package com.granatum.feature.inventory.presentation

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.Dimensions
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.ProductDraft
import com.granatum.feature.inventory.domain.model.ProductHistoryEntry
import com.granatum.feature.inventory.domain.model.ProductModel
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.model.Warehouse
import com.granatum.feature.inventory.domain.repository.InventoryRepository
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

val VASES = CategoryModel("c1", "Jarrones", null)
val RIBBONS = CategoryModel("c2", "Cintas", "De raso")

fun product(
    id: String,
    description: String = "Producto $id",
    category: CategoryModel = VASES,
    quantity: Int = 5,
    location: StorageLocation = StorageLocation(Warehouse.A, 1),
    updated: Long = 0
) = ProductModel(
    id = id, category = category, description = description, dimensions = Dimensions(20.0, 10.0), quantity = quantity,
    imageUrl = "https://example.test/$id.jpg", location = location,
    createdAt = Instant.fromEpochSeconds(0), updatedAt = Instant.fromEpochSeconds(updated)
)

fun useCases(repo: InventoryRepository) = InventoryUseCases(repo)

/** In-memory repository: records what the screens ask for and answers with [failure] when set. */
class FakeInventoryRepository(
    products: List<ProductModel> = emptyList(),
    categories: List<CategoryModel> = listOf(VASES, RIBBONS)
) : InventoryRepository {
    val products = MutableStateFlow(products)
    val categories = MutableStateFlow(categories)
    var failure: InventoryError? = null
    var refreshes = 0
    val created = mutableListOf<ProductDraft>()
    val updated = mutableListOf<Pair<String, ProductDraft>>()
    val quantities = mutableListOf<Triple<String, Int, String?>>()
    val deletedCategories = mutableListOf<String>()
    val images = mutableListOf<Pair<String, String>>()
    val removedImages = mutableListOf<String>()

    /** Answers image uploads with this instead of [failure]; [imageGate], when set, holds the answer. */
    var imageFailure: InventoryError? = null
    var imageGate: CompletableDeferred<Unit>? = null

    override fun observeProducts(): Flow<List<ProductModel>> = products
    override fun observeProduct(id: String): Flow<ProductModel?> = products.map { list -> list.firstOrNull { it.id == id } }
    override fun observeCategories(): Flow<List<CategoryModel>> = categories
    override fun observeHistory(productId: String): Flow<List<ProductHistoryEntry>> = MutableStateFlow(emptyList())

    private fun <T> answer(value: () -> T): Result<T, InventoryError> = failure?.let { Result.Failure(it) } ?: Result.Success(value())

    override suspend fun refresh(): EmptyResult<InventoryError> { refreshes++; return answer { } }
    override suspend fun refreshProduct(id: String): EmptyResult<InventoryError> = answer { }
    override suspend fun refreshHistory(productId: String): EmptyResult<InventoryError> = answer { }

    override suspend fun create(draft: ProductDraft) = answer { created += draft; product("new", draft.description) }
    override suspend fun update(id: String, draft: ProductDraft) = answer { updated += id to draft; product(id, draft.description) }
    override suspend fun setQuantity(id: String, quantity: Int, reason: String?) = answer {
        quantities += Triple(id, quantity, reason)
        products.update { list -> list.map { if (it.id == id) it.copy(quantity = quantity) else it } }
        products.value.first { it.id == id }
    }
    override suspend fun delete(id: String): EmptyResult<InventoryError> = answer { products.update { list -> list.filterNot { it.id == id } } }

    override suspend fun setImage(id: String, bytes: ByteArray, fileName: String, mimeType: String): Result<ProductModel, InventoryError> {
        images += id to fileName
        imageGate?.await()
        return imageFailure?.let { Result.Failure(it) } ?: Result.Success(product(id).copy(imageUrl = "https://s.test/$id/$fileName?token=t"))
    }

    override suspend fun removeImage(id: String): Result<ProductModel, InventoryError> {
        removedImages += id
        return imageFailure?.let { Result.Failure(it) } ?: Result.Success(product(id).copy(imageUrl = null))
    }

    override suspend fun createCategory(name: String, description: String?) = answer {
        CategoryModel("c${categories.value.size + 1}", name, description).also { c -> categories.update { it + c } }
    }
    override suspend fun updateCategory(id: String, name: String, description: String?) = answer {
        CategoryModel(id, name, description).also { c -> categories.update { list -> list.map { if (it.id == id) c else it } } }
    }
    override suspend fun deleteCategory(id: String): EmptyResult<InventoryError> = answer {
        deletedCategories += id
        categories.update { list -> list.filterNot { it.id == id } }
    }
}
