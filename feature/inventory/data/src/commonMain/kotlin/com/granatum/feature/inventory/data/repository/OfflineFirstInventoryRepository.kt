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
import com.granatum.feature.inventory.domain.model.HistoryChangeType
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.MaterialCondition
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialHistoryEntry
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.MaterialSize
import com.granatum.feature.inventory.domain.model.SizeUnit
import com.granatum.feature.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * Reads from the local copy, which every successful call keeps up to date; writes go to the
 * server first and touch the copy only with the server's answer. Without coverage nothing is
 * written anywhere (specs/006-inventario-real, research D1).
 */
class OfflineFirstInventoryRepository(
    private val remote: InventoryRemoteDataSource,
    private val dao: InventoryDao
) : InventoryRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeCategories(): Flow<List<CategoryModel>> =
        dao.observeCategories().map { list -> list.map { it.toDomain() } }

    override fun observeMaterials(): Flow<List<MaterialModel>> =
        combine(dao.observeMaterials(), dao.observeCategories()) { materials, categories ->
            val byId = categories.associateBy { it.id }
            materials.mapNotNull { m -> byId[m.categoryId]?.let { m.toDomain(it.toDomain()) } }
        }

    override fun observeMaterial(id: String): Flow<MaterialModel?> =
        combine(dao.observeMaterial(id), dao.observeCategories()) { material, categories ->
            material?.let { m -> categories.firstOrNull { it.id == m.categoryId }?.let { m.toDomain(it.toDomain()) } }
        }

    override fun observeHistory(materialId: String): Flow<List<MaterialHistoryEntry>> =
        dao.observeHistory(materialId).map { list -> list.map { it.toDomain() } }

    override suspend fun refresh(): EmptyResult<InventoryError> {
        val categories = when (val r = remote.categories()) { is Result.Failure -> return r; is Result.Success -> r.data }
        val materials = when (val r = remote.materials()) { is Result.Failure -> return r; is Result.Success -> r.data }
        // A material carries its category; the list of categories wins when both are present.
        val allCategories = (categories + materials.map { it.categoria }).distinctBy { it.id }
        dao.replaceAll(allCategories.map { it.toEntity() }, materials.map { it.toEntity() })
        return Result.Success(Unit)
    }

    override suspend fun refreshMaterial(id: String): EmptyResult<InventoryError> =
        cacheMaterial(id, remote.material(id))

    override suspend fun refreshHistory(materialId: String): EmptyResult<InventoryError> =
        when (val r = remote.history(materialId)) {
            is Result.Success -> { dao.replaceHistory(materialId, r.data.map { it.toEntity() }); Result.Success(Unit) }
            is Result.Failure -> { forgetIfGone(materialId, r.error); r }
        }

    override suspend fun create(draft: MaterialDraft): Result<MaterialModel, InventoryError> {
        val body = CreateMaterialRequestDto(
            nombre = draft.name.trim(), categoriaId = requireNotNull(draft.categoryId),
            cantidadDisponible = draft.available, cantidadTotal = draft.total, tamano = draft.size.toDto(),
            color = draft.color.trim(), materialFisico = draft.physicalMaterial.trim(), estado = draft.condition.name,
            ubicacion = draft.location.trim(), precioUnitario = draft.unitPrice, proveedor = draft.supplier.trim(),
            fotos = draft.photos
        )
        return saved(remote.create(body))
    }

    override suspend fun update(id: String, draft: MaterialDraft): Result<MaterialModel, InventoryError> {
        val body = UpdateMaterialRequestDto(
            nombre = draft.name.trim(), categoriaId = requireNotNull(draft.categoryId), cantidadTotal = draft.total,
            tamano = draft.size.toDto(), color = draft.color.trim(), materialFisico = draft.physicalMaterial.trim(),
            estado = draft.condition.name, ubicacion = draft.location.trim(), precioUnitario = draft.unitPrice,
            proveedor = draft.supplier.trim(),
            // Required, and sent back as they were: the server offers no upload (research D6).
            fotos = draft.photos
        )
        return saved(remote.update(id, body), id)
    }

    override suspend fun adjustQuantity(id: String, available: Int, reason: String): Result<MaterialModel, InventoryError> =
        saved(remote.adjust(id, UpdateCantidadRequestDto(available, reason.trim())), id)

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

    private suspend fun saved(result: Result<MaterialDto, InventoryError>, id: String? = null): Result<MaterialModel, InventoryError> =
        when (result) {
            is Result.Success -> {
                val dto = result.data
                dao.upsertCategory(dto.categoria.toEntity())
                dao.upsertMaterial(dto.toEntity())
                Result.Success(dto.toEntity().toDomain(dto.categoria.toEntity().toDomain()))
            }
            is Result.Failure -> { id?.let { forgetIfGone(it, result.error) }; result }
        }

    private suspend fun cacheMaterial(id: String, result: Result<MaterialDto, InventoryError>): EmptyResult<InventoryError> =
        when (val r = saved(result, id)) { is Result.Success -> Result.Success(Unit); is Result.Failure -> r }

    private suspend fun savedCategory(result: Result<CategoriaDto, InventoryError>): Result<CategoryModel, InventoryError> =
        when (result) {
            is Result.Success -> { dao.upsertCategory(result.data.toEntity()); Result.Success(result.data.toEntity().toDomain()) }
            is Result.Failure -> result
        }

    /** Deleted by someone else: stop showing it (FR-015). */
    private suspend fun forgetIfGone(id: String, error: InventoryError) {
        if (error == InventoryError.MaterialNotFound) dao.deleteMaterial(id)
    }

    // Mapping

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

    private fun MaterialEntity.toDomain(category: CategoryModel) = MaterialModel(
        id = id, name = name, category = category, available = available, total = total,
        size = MaterialSize(height, width, diameter, enumOr(unit, SizeUnit.CM)),
        color = color, physicalMaterial = physicalMaterial, condition = enumOr(condition, MaterialCondition.USADO),
        location = location, unitPrice = unitPrice, supplier = supplier,
        photos = runCatching { json.decodeFromString<List<String>>(photosJson) }.getOrDefault(emptyList()),
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
        updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMillis)
    )

    private fun HistorialMaterialDto.toEntity() = MaterialHistoryEntity(
        id, materialId, tipoCambio, valorAnterior, valorNuevo, motivo, Instant.parse(fecha).toEpochMilliseconds()
    )

    private fun MaterialHistoryEntity.toDomain() = MaterialHistoryEntry(
        id, enumOr(type, HistoryChangeType.OTRO), previousValue, newValue, reason, Instant.fromEpochMilliseconds(atEpochMillis)
    )

    private fun MaterialSize.toDto() = TamanoDto(alto = height, ancho = width, unidadMedida = unit.name, diametro = diameter)

    private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
