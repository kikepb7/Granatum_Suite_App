package com.granatum.feature.inventory.presentation

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.MaterialCondition
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialHistoryEntry
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.MaterialSize
import com.granatum.feature.inventory.domain.model.SizeUnit
import com.granatum.feature.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

val VASES = CategoryModel("c1", "Jarrones", null)
val RIBBONS = CategoryModel("c2", "Cintas", "De raso")

fun material(id: String, name: String, category: CategoryModel = VASES, available: Int = 5, total: Int = 10, condition: MaterialCondition = MaterialCondition.NUEVO) = MaterialModel(
    id = id, name = name, category = category, available = available, total = total,
    size = MaterialSize(20.0, 10.0, null, SizeUnit.CM), color = "Blanco", physicalMaterial = "Cristal",
    condition = condition, location = "Almacén", unitPrice = 12.5, supplier = "Flores SL",
    photos = listOf("https://example.test/a.jpg"), createdAt = Instant.fromEpochSeconds(0), updatedAt = Instant.fromEpochSeconds(0)
)

/** In-memory repository: records what the screens ask for and answers with [failure] when set. */
class FakeInventoryRepository(
    materials: List<MaterialModel> = emptyList(),
    categories: List<CategoryModel> = listOf(VASES, RIBBONS)
) : InventoryRepository {
    val materials = MutableStateFlow(materials)
    val categories = MutableStateFlow(categories)
    var failure: InventoryError? = null
    var refreshes = 0
    val created = mutableListOf<MaterialDraft>()
    val updated = mutableListOf<Pair<String, MaterialDraft>>()
    val adjusted = mutableListOf<Triple<String, Int, String>>()
    val deletedCategories = mutableListOf<String>()

    override fun observeMaterials(): Flow<List<MaterialModel>> = materials
    override fun observeMaterial(id: String): Flow<MaterialModel?> = materials.map { list -> list.firstOrNull { it.id == id } }
    override fun observeCategories(): Flow<List<CategoryModel>> = categories
    override fun observeHistory(materialId: String): Flow<List<MaterialHistoryEntry>> = MutableStateFlow(emptyList())

    private fun <T> answer(value: () -> T): Result<T, InventoryError> = failure?.let { Result.Failure(it) } ?: Result.Success(value())

    override suspend fun refresh(): EmptyResult<InventoryError> { refreshes++; return answer { } }
    override suspend fun refreshMaterial(id: String): EmptyResult<InventoryError> = answer { }
    override suspend fun refreshHistory(materialId: String): EmptyResult<InventoryError> = answer { }

    override suspend fun create(draft: MaterialDraft) = answer { created += draft; material("new", draft.name) }
    override suspend fun update(id: String, draft: MaterialDraft) = answer { updated += id to draft; material(id, draft.name) }
    override suspend fun adjustQuantity(id: String, available: Int, reason: String) = answer {
        adjusted += Triple(id, available, reason)
        materials.update { list -> list.map { if (it.id == id) it.copy(available = available) else it } }
        materials.value.first { it.id == id }
    }
    override suspend fun delete(id: String): EmptyResult<InventoryError> = answer { materials.update { list -> list.filterNot { it.id == id } } }

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
