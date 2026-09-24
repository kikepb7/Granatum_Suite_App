package com.granatum.feature.inventory.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class SaveMaterialRequestDto(
    val name: String,
    val category: String,
    val quantity: Int,
    val size: String?,
    val color: String?,
    val location: String?,
    val photoUrls: List<String>,
    val notes: String?
)

@Serializable
data class UpdateQuantityRequestDto(
    val newQuantity: Int,
    val reason: String
)
