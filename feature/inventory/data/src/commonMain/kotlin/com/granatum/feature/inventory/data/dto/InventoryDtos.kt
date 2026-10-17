package com.granatum.feature.inventory.data.dto

import kotlinx.serialization.Serializable

// Field names match docs/openapi.json letter for letter (constitution principle VI).

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CategoryRequestDto(val name: String, val description: String? = null)

/** Centimetres. */
@Serializable
data class DimensionsDto(val height: Double, val width: Double)

/** `warehouse` is `A`, `B` or `C`; `section` 1 to 5. */
@Serializable
data class LocationDto(val warehouse: String, val section: Int)

@Serializable
data class MaterialDto(
    val id: String,
    val category: CategoryDto,
    val description: String,
    val dimensions: DimensionsDto,
    val quantity: Int,
    val location: LocationDto,
    /** Temporary signed URL; null without image or when the server could not sign it. */
    val imageUrl: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateMaterialRequestDto(
    val categoryId: String,
    val description: String,
    val dimensions: DimensionsDto,
    val quantity: Int,
    val location: LocationDto
)

/** No `quantity`: after creation it only changes through `PATCH …/quantity`, with a reason. */
@Serializable
data class UpdateMaterialRequestDto(
    val categoryId: String,
    val description: String,
    val dimensions: DimensionsDto,
    val location: LocationDto
)

/** `reason` is required (not blank). */
@Serializable
data class UpdateQuantityRequestDto(val quantity: Int, val reason: String)

@Serializable
data class MaterialHistoryDto(
    val id: String,
    val materialId: String,
    val changeType: String,
    val reason: String,
    val date: String,
    val userId: String,
    val previousValue: String? = null,
    val newValue: String? = null
)
