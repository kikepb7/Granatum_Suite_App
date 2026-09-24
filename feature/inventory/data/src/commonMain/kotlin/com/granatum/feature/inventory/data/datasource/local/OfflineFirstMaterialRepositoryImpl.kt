package com.granatum.feature.inventory.data.datasource.local

import com.granatum.core.data.networking.get
import com.granatum.core.data.networking.post
import com.granatum.core.data.networking.put
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.asEmptyResult
import com.granatum.core.domain.util.map
import com.granatum.core.domain.util.onSuccess
import com.granatum.feature.inventory.data.dto.MaterialDto
import com.granatum.feature.inventory.data.dto.request.SaveMaterialRequestDto
import com.granatum.feature.inventory.data.dto.request.UpdateQuantityRequestDto
import com.granatum.feature.inventory.data.mappers.toDomain
import com.granatum.feature.inventory.data.mappers.toEntity
import com.granatum.feature.inventory.data.mappers.toRequestDto
import com.granatum.feature.inventory.database.AppInventoryDatabase
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.StockMovementModel
import com.granatum.feature.inventory.domain.repository.MaterialRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstMaterialRepositoryImpl(
    private val httpClient: HttpClient,
    private val db: AppInventoryDatabase
) : MaterialRepository {

    override fun observeMaterials(): Flow<List<MaterialModel>> =
        db.materialDao.observeAllMaterials().map { entities -> entities.map { it.toDomain() } }

    override fun observeMaterial(materialId: String): Flow<MaterialModel?> =
        db.materialDao.observeMaterial(materialId = materialId).map { it?.toDomain() }

    override fun observeStockHistory(materialId: String): Flow<List<StockMovementModel>> =
        db.materialDao.observeStockHistory(materialId = materialId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshMaterials(): EmptyResult<DataError.Remote> =
        httpClient.get<List<MaterialDto>>(route = "/inventory/materials")
            .onSuccess { materials -> db.materialDao.syncMaterials(materials = materials.map { it.toEntity() }) }
            .asEmptyResult()

    override suspend fun saveMaterial(draft: MaterialDraft): Result<MaterialModel, DataError.Remote> {
        val requestDto = draft.toRequestDto()
        val result = if (draft.id == null) {
            httpClient.post<SaveMaterialRequestDto, MaterialDto>(route = "/inventory/materials", body = requestDto)
        } else {
            httpClient.put<SaveMaterialRequestDto, MaterialDto>(
                route = "/inventory/materials/${draft.id}",
                body = requestDto
            )
        }
        return result
            .onSuccess { dto -> db.materialDao.upsertMaterial(material = dto.toEntity()) }
            .map { it.toDomain() }
    }

    override suspend fun updateQuantity(
        materialId: String,
        newQuantity: Int,
        reason: String
    ): Result<MaterialModel, DataError.Remote> =
        httpClient.put<UpdateQuantityRequestDto, MaterialDto>(
            route = "/inventory/materials/$materialId/quantity",
            body = UpdateQuantityRequestDto(newQuantity = newQuantity, reason = reason)
        )
            .onSuccess { dto ->
                db.materialDao.upsertMaterial(material = dto.toEntity())
                // The backend is expected to also return the created movement so the local
                // history cache stays in sync without a second round trip; if that changes,
                // fetch StockMovementDto separately here and upsert it the same way.
            }
            .map { it.toDomain() }
}
