package com.granatum.feature.inventory.data.repository

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.data.dto.CategoriaDto
import com.granatum.feature.inventory.data.dto.CategoriaRequestDto
import com.granatum.feature.inventory.data.dto.CreateMaterialRequestDto
import com.granatum.feature.inventory.data.dto.HistorialMaterialDto
import com.granatum.feature.inventory.data.dto.MaterialDto
import com.granatum.feature.inventory.data.dto.TamanoDto
import com.granatum.feature.inventory.data.dto.UpdateCantidadRequestDto
import com.granatum.feature.inventory.data.dto.UpdateMaterialRequestDto
import com.granatum.feature.inventory.data.remote.InventoryRemoteDataSource
import com.granatum.feature.inventory.database.dao.InventoryDao
import com.granatum.feature.inventory.database.entity.CategoryEntity
import com.granatum.feature.inventory.database.entity.MaterialEntity
import com.granatum.feature.inventory.database.entity.MaterialHistoryEntity
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.Dimensions
import com.granatum.feature.inventory.domain.model.HistoryChangeType
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.ProductDraft
import com.granatum.feature.inventory.domain.model.ProductHistoryEntry
import com.granatum.feature.inventory.domain.model.ProductModel
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * Reads from the local copy, which every successful call keeps up to date; writes go to the
 * server first and touch the copy only with the server's answer. Without coverage nothing is
 * written anywhere.
 *
 * The product (specs/011-inventario-productos) is mapped onto the server's material, which does not
 * change (research D1): the description is its `nombre`, the location its `ubicacion` (`A-3`), the
 * image its first photo and the quantity both its available and total amounts. What the server
 * still requires and the product no longer has is kept as it was when editing (it is in the local
 * copy) and gets neutral values when creating.
 */
class OfflineFirstInventoryRepository(
    private val remote: InventoryRemoteDataSource,
    private val dao: InventoryDao
) : InventoryRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeCategories(): Flow<List<CategoryModel>> =
        dao.observeCategories().map { list -> list.map { it.toDomain() } }

    override fun observeProducts(): Flow<List<ProductModel>> =
        combine(dao.observeMaterials(), dao.observeCategories()) { materials, categories ->
            val byId = categories.associateBy { it.id }
            materials.mapNotNull { m -> byId[m.categoryId]?.let { m.toDomain(it.toDomain()) } }
        }

    override fun observeProduct(id: String): Flow<ProductModel?> =
        combine(dao.observeMaterial(id), dao.observeCategories()) { material, categories ->
            material?.let { m -> categories.firstOrNull { it.id == m.categoryId }?.let { m.toDomain(it.toDomain()) } }
        }

    override fun observeHistory(productId: String): Flow<List<ProductHistoryEntry>> =
        dao.observeHistory(productId).map { list -> list.map { it.toDomain() }.sortedByDescending { it.at } }

    override suspend fun refresh(): EmptyResult<InventoryError> {
        val categories = when (val r = remote.categories()) { is Result.Failure -> return r; is Result.Success -> r.data }
        val materials = when (val r = remote.materials()) { is Result.Failure -> return r; is Result.Success -> r.data }
        // A material carries its category; the list of categories wins when both are present.
        val allCategories = (categories + materials.map { it.categoria }).distinctBy { it.id }
        dao.replaceAll(allCategories.map { it.toEntity() }, materials.map { it.toEntity() })
        return Result.Success(Unit)
    }

    override suspend fun refreshProduct(id: String): EmptyResult<InventoryError> =
        when (val r = saved(remote.material(id), id)) { is Result.Success -> Result.Success(Unit); is Result.Failure -> r }

    override suspend fun refreshHistory(productId: String): EmptyResult<InventoryError> =
        when (val r = remote.history(productId)) {
            is Result.Success -> { dao.replaceHistory(productId, r.data.map { it.toEntity() }); Result.Success(Unit) }
            is Result.Failure -> { forgetIfGone(productId, r.error); r }
        }

    override suspend fun create(draft: ProductDraft): Result<ProductModel, InventoryError> {
        val body = CreateMaterialRequestDto(
            nombre = draft.description.trim(),
            categoriaId = requireNotNull(draft.categoryId),
            cantidadDisponible = draft.quantity,
            cantidadTotal = draft.quantity,
            tamano = draft.dimensions.toDto(diameter = null),
            color = NEUTRAL_TEXT,
            materialFisico = NEUTRAL_TEXT,
            estado = NEUTRAL_CONDITION,
            ubicacion = draft.location?.toApi().orEmpty(),
            precioUnitario = 0.0,
            proveedor = NEUTRAL_TEXT,
            fotos = listOfNotNull(draft.imageUrl)
        )
        return saved(remote.create(body))
    }

    override suspend fun update(id: String, draft: ProductDraft): Result<ProductModel, InventoryError> {
        val current = dao.observeMaterial(id).first() ?: return Result.Failure(InventoryError.ProductNotFound)
        return saved(remote.update(id, current.updateBody(draft, total = current.total)), id)
    }

    /**
     * The server records history only for the available amount, which may not exceed the total
     * (research D2): raising sets the total first, lowering the available first, so both end equal.
     */
    override suspend fun setQuantity(id: String, quantity: Int, reason: String?): Result<ProductModel, InventoryError> {
        val current = dao.observeMaterial(id).first() ?: return Result.Failure(InventoryError.ProductNotFound)
        val motive = reason?.trim()?.ifEmpty { null } ?: DEFAULT_REASON
        val keep = current.toDraft()
        if (quantity > current.total) {
            when (val r = saved(remote.update(id, current.updateBody(keep, total = quantity)), id)) { is Result.Failure -> return r; else -> Unit }
        }
        val adjusted = saved(remote.adjust(id, UpdateCantidadRequestDto(quantity, motive)), id)
        if (adjusted is Result.Failure || quantity >= current.total) return adjusted
        val now = dao.observeMaterial(id).first() ?: return adjusted
        return saved(remote.update(id, now.updateBody(keep, total = quantity)), id)
    }

    override suspend fun delete(id: String): EmptyResult<InventoryError> = when (val r = remote.delete(id)) {
        is Result.Success -> { dao.deleteMaterial(id); r }
        is Result.Failure -> { forgetIfGone(id, r.error); r }
    }

    override suspend fun createCategory(name: String, description: String?): Result<CategoryModel, InventoryError> =
        savedCategory(remote.createCategory(CategoriaRequestDto(name.trim(), description?.trim()?.ifBlank { null })))

    override suspend fun updateCategory(id: String, name: String, description: String?): Result<CategoryModel, InventoryError> =
        savedCategory(remote.updateCategory(id, CategoriaRequestDto(name.trim(), description?.trim()?.ifBlank { null })))

    override suspend fun deleteCategory(id: String): EmptyResult<InventoryError> = when (val r = remote.deleteCategory(id)) {
        is Result.Success -> { dao.deleteCategory(id); r }
        is Result.Failure -> { if (r.error == InventoryError.CategoryNotFound) dao.deleteCategory(id); r }
    }

    private suspend fun saved(result: Result<MaterialDto, InventoryError>, id: String? = null): Result<ProductModel, InventoryError> =
        when (result) {
            is Result.Success -> {
                val dto = result.data
                dao.upsertCategory(dto.categoria.toEntity())
                dao.upsertMaterial(dto.toEntity())
                Result.Success(dto.toEntity().toDomain(dto.categoria.toEntity().toDomain()))
            }
            is Result.Failure -> { id?.let { forgetIfGone(it, result.error) }; result }
        }

    private suspend fun savedCategory(result: Result<CategoriaDto, InventoryError>): Result<CategoryModel, InventoryError> =
        when (result) {
            is Result.Success -> { dao.upsertCategory(result.data.toEntity()); Result.Success(result.data.toEntity().toDomain()) }
            is Result.Failure -> result
        }

    /** Deleted by someone else: stop showing it. */
    private suspend fun forgetIfGone(id: String, error: InventoryError) {
        if (error == InventoryError.ProductNotFound) dao.deleteMaterial(id)
    }

    // Mapping (research D1)

    /** The full body the server wants: the product's fields, the rest kept as they were. */
    private fun MaterialEntity.updateBody(draft: ProductDraft, total: Int) = UpdateMaterialRequestDto(
        nombre = draft.description.trim(),
        categoriaId = draft.categoryId ?: categoryId,
        cantidadTotal = total,
        tamano = draft.dimensions.toDto(diameter = diameter?.let { it * centimetresPer(unit) }),
        color = color,
        materialFisico = physicalMaterial,
        estado = condition,
        ubicacion = draft.location?.toApi() ?: location,
        precioUnitario = unitPrice,
        proveedor = supplier,
        // The image replaces the first photo; any others the server kept stay behind it.
        fotos = listOfNotNull(draft.imageUrl) + photos().drop(1)
    )

    private fun MaterialEntity.toDraft() = ProductDraft(
        categoryId = categoryId,
        description = name,
        dimensions = Dimensions(height * centimetresPer(unit), width * centimetresPer(unit)),
        quantity = available,
        imageUrl = photos().firstOrNull(),
        location = StorageLocation.parse(location)
    )

    private fun MaterialEntity.photos(): List<String> =
        runCatching { json.decodeFromString<List<String>>(photosJson) }.getOrDefault(emptyList())

    private fun CategoriaDto.toEntity() = CategoryEntity(id, nombre, descripcion)
    private fun CategoryEntity.toDomain() = CategoryModel(id, name, description)

    private fun MaterialDto.toEntity() = MaterialEntity(
        id = id, name = nombre, categoryId = categoria.id, available = cantidadDisponible, total = cantidadTotal,
        height = tamano.alto, width = tamano.ancho, diameter = tamano.diametro, unit = tamano.unidadMedida,
        color = color, physicalMaterial = materialFisico, condition = estado, location = ubicacion,
        unitPrice = precioUnitario, supplier = proveedor, photosJson = json.encodeToString(fotos),
        createdAtEpochMillis = Instant.parse(fechaAlta).toEpochMilliseconds(),
        updatedAtEpochMillis = Instant.parse(fechaUltimaModificacion).toEpochMilliseconds()
    )

    private fun MaterialEntity.toDomain(category: CategoryModel) = ProductModel(
        id = id,
        category = category,
        description = name,
        dimensions = Dimensions(height * centimetresPer(unit), width * centimetresPer(unit)),
        quantity = available,
        imageUrl = photos().firstOrNull(),
        location = StorageLocation.parse(location),
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
        updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMillis)
    )

    private fun HistorialMaterialDto.toEntity() = MaterialHistoryEntity(
        id, materialId, tipoCambio, valorAnterior, valorNuevo, motivo, Instant.parse(fecha).toEpochMilliseconds()
    )

    private fun MaterialHistoryEntity.toDomain() = ProductHistoryEntry(
        id, enumValues<HistoryChangeType>().firstOrNull { it.name == type } ?: HistoryChangeType.OTRO,
        previousValue, newValue, reason, Instant.fromEpochMilliseconds(atEpochMillis)
    )

    private fun Dimensions.toDto(diameter: Double?) = TamanoDto(alto = height, ancho = width, unidadMedida = CENTIMETRES, diametro = diameter)

    private companion object {
        const val CENTIMETRES = "CM"
        const val NEUTRAL_TEXT = "—"
        const val NEUTRAL_CONDITION = "NUEVO"
        const val DEFAULT_REASON = "Ajuste de cantidad"

        /** Products are measured in centimetres; older ones may be in another unit. */
        fun centimetresPer(unit: String): Double = when (unit) {
            "MM" -> 0.1
            "M" -> 100.0
            "IN" -> 2.54
            else -> 1.0
        }
    }
}
