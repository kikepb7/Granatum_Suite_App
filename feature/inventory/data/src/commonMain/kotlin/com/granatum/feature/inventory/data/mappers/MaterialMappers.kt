package com.granatum.feature.inventory.data.mappers

import com.granatum.feature.inventory.data.dto.MaterialDto
import com.granatum.feature.inventory.data.dto.StockMovementDto
import com.granatum.feature.inventory.data.dto.request.SaveMaterialRequestDto
import com.granatum.feature.inventory.database.entity.MaterialEntity
import com.granatum.feature.inventory.database.entity.StockMovementEntity
import com.granatum.feature.inventory.domain.model.MaterialCategory
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.MaterialStatus
import com.granatum.feature.inventory.domain.model.StockMovementModel
import kotlinx.datetime.Instant

private const val PHOTO_URL_SEPARATOR = "|"

private fun String.toMaterialCategory(): MaterialCategory =
    runCatching { MaterialCategory.valueOf(this) }.getOrDefault(MaterialCategory.OTROS)

private fun String.toMaterialStatus(): MaterialStatus =
    runCatching { MaterialStatus.valueOf(this) }.getOrDefault(MaterialStatus.DISPONIBLE)

fun MaterialDto.toEntity(): MaterialEntity = MaterialEntity(
    materialId = id,
    name = name,
    category = category,
    status = status,
    quantity = quantity,
    size = size,
    color = color,
    location = location,
    photoUrlsCsv = photoUrls.joinToString(separator = PHOTO_URL_SEPARATOR),
    notes = notes,
    updatedAtEpochMillis = Instant.parse(updatedAt).toEpochMilliseconds()
)

fun MaterialEntity.toDomain(): MaterialModel = MaterialModel(
    id = materialId,
    name = name,
    category = category.toMaterialCategory(),
    status = status.toMaterialStatus(),
    quantity = quantity,
    size = size,
    color = color,
    location = location,
    photoUrls = if (photoUrlsCsv.isBlank()) emptyList() else photoUrlsCsv.split(PHOTO_URL_SEPARATOR),
    notes = notes,
    updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMillis)
)

fun MaterialDto.toDomain(): MaterialModel = toEntity().toDomain()

fun MaterialDraft.toRequestDto(): SaveMaterialRequestDto = SaveMaterialRequestDto(
    name = name,
    category = category.name,
    quantity = quantity,
    size = size,
    color = color,
    location = location,
    photoUrls = photoUrls,
    notes = notes
)

fun StockMovementDto.toEntity(): StockMovementEntity = StockMovementEntity(
    movementId = id,
    materialId = materialId,
    previousQuantity = previousQuantity,
    newQuantity = newQuantity,
    reason = reason,
    changedByUsername = changedByUsername,
    changedAtEpochMillis = Instant.parse(changedAt).toEpochMilliseconds()
)

fun StockMovementEntity.toDomain(): StockMovementModel = StockMovementModel(
    id = movementId,
    materialId = materialId,
    previousQuantity = previousQuantity,
    newQuantity = newQuantity,
    reason = reason,
    changedByUsername = changedByUsername,
    changedAt = Instant.fromEpochMilliseconds(changedAtEpochMillis)
)
