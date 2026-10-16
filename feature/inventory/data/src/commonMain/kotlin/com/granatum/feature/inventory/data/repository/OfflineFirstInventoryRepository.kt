package com.granatum.feature.inventory.data.repository

import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.data.dto.CategoriaDto
import com.granatum.feature.inventory.data.dto.CategoriaRequestDto
import com.granatum.feature.inventory.data.dto.CreateMaterialRequestDto
import com.granatum.feature.inventory.data.dto.DimensionesDto
import com.granatum.feature.inventory.data.dto.HistorialMaterialDto
import com.granatum.feature.inventory.data.dto.LocalizacionDto
import com.granatum.feature.inventory.data.dto.MaterialDto
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
import com.granatum.feature.inventory.domain.model.Warehouse
import com.granatum.feature.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.roundToLong
import kotlin.time.Instant

/**
 * Reads from the local copy, which every successful call keeps up to date; writes go to the
 * server first and touch the copy only with the server's answer. Without coverage nothing is
 * written anywhere.
 *
 * The product (specs/011-inventario-productos) is the server's material of backend feature 010
 * field for field; the image travels apart, as a multipart upload the server stores itself.
 */
class OfflineFirstInventoryRepository(
    private val remote: InventoryRemoteDataSource,
    private val dao: InventoryDao
) : InventoryRepository {

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
        val location = draft.location ?: return Result.Failure(InventoryError.Invalid)
        val body = CreateMaterialRequestDto(
            categoriaId = draft.categoryId ?: return Result.Failure(InventoryError.Invalid),
            descripcion = draft.description.trim(),
            dimensiones = draft.dimensions.toDto(),
            cantidad = draft.quantity,
            localizacion = location.toDto()
        )
        return saved(remote.create(body))
    }

    /** Everything but the quantity, which only changes with a reason ([setQuantity]). */
    override suspend fun update(id: String, draft: ProductDraft): Result<ProductModel, InventoryError> {
        val location = draft.location ?: return Result.Failure(InventoryError.Invalid)
        val body = UpdateMaterialRequestDto(
            categoriaId = draft.categoryId ?: return Result.Failure(InventoryError.Invalid),
            descripcion = draft.description.trim(),
            dimensiones = draft.dimensions.toDto(),
            localizacion = location.toDto()
        )
        return saved(remote.update(id, body), id)
    }

    /** The server records the change in the history; it requires a reason, so one is always sent. */
    override suspend fun setQuantity(id: String, quantity: Int, reason: String?): Result<ProductModel, InventoryError> {
        val motive = reason?.trim()?.ifEmpty { null } ?: DEFAULT_REASON
        return saved(remote.adjust(id, UpdateCantidadRequestDto(quantity, motive)), id)
    }

    override suspend fun delete(id: String): EmptyResult<InventoryError> = when (val r = remote.delete(id)) {
        is Result.Success -> { dao.deleteMaterial(id); r }
        is Result.Failure -> { forgetIfGone(id, r.error); r }
    }

    override suspend fun setImage(id: String, bytes: ByteArray, fileName: String, mimeType: String): Result<ProductModel, InventoryError> =
        saved(remote.uploadImage(id, bytes, fileName, mimeType), id)

    override suspend fun removeImage(id: String): Result<ProductModel, InventoryError> = saved(remote.removeImage(id), id)

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
                dto.toEntity().toDomain(dto.categoria.toEntity().toDomain())?.let { Result.Success(it) } ?: Result.Failure(InventoryError.Unknown)
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

    // Mapping

    private fun CategoriaDto.toEntity() = CategoryEntity(id, nombre, descripcion)
    private fun CategoryEntity.toDomain() = CategoryModel(id, name, description)

    private fun MaterialDto.toEntity() = MaterialEntity(
        id = id, categoryId = categoria.id, description = descripcion,
        height = dimensiones.alto, width = dimensiones.ancho, quantity = cantidad,
        warehouse = localizacion.almacen, section = localizacion.seccion, imageUrl = imagenUrl,
        createdAtEpochMillis = Instant.parse(fechaAlta).toEpochMilliseconds(),
        updatedAtEpochMillis = Instant.parse(fechaUltimaModificacion).toEpochMilliseconds()
    )

    /** Null for a location the app does not know, so one odd row does not hide the whole list. */
    private fun MaterialEntity.toDomain(category: CategoryModel): ProductModel? {
        val location = Warehouse.entries.firstOrNull { it.name == warehouse }
            ?.takeIf { section in StorageLocation.SECTIONS }
            ?.let { StorageLocation(it, section) } ?: return null
        return ProductModel(
            id = id,
            category = category,
            description = description,
            dimensions = Dimensions(height, width),
            quantity = quantity,
            imageUrl = imageUrl,
            location = location,
            createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
            updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMillis)
        )
    }

    private fun HistorialMaterialDto.toEntity() = MaterialHistoryEntity(
        id, materialId, tipoCambio, valorAnterior, valorNuevo, motivo, Instant.parse(fecha).toEpochMilliseconds()
    )

    private fun MaterialHistoryEntity.toDomain() = ProductHistoryEntry(
        id, enumValues<HistoryChangeType>().firstOrNull { it.name == type } ?: HistoryChangeType.OTRO,
        previousValue, newValue, reason, Instant.fromEpochMilliseconds(atEpochMillis)
    )

    /** The server takes up to two decimals. */
    private fun Dimensions.toDto() = DimensionesDto(alto = height.twoDecimals(), ancho = width.twoDecimals())

    private fun Double.twoDecimals() = (this * 100).roundToLong() / 100.0

    private fun StorageLocation.toDto() = LocalizacionDto(almacen = warehouse.name, seccion = section)

    private companion object {
        /** The server requires a reason; the person may leave it empty. */
        const val DEFAULT_REASON = "Ajuste de cantidad"
    }
}
