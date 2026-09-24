package com.granatum.feature.inventory.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MaterialDto(
    val id: String,
    val name: String,
    val category: String,
    val status: String,
    val quantity: Int,
    val size: String? = null,
    val color: String? = null,
    val location: String? = null,
    val photoUrls: List<String> = emptyList(),
    val notes: String? = null,
    val updatedAt: String
)

@Serializable
data class StockMovementDto(
    val id: String,
    val materialId: String,
    val previousQuantity: Int,
    val newQuantity: Int,
    val reason: String,
    val changedByUsername: String,
    val changedAt: String
)
