package com.granatum.feature.inventory.domain.model

import kotlinx.datetime.Instant

data class StockMovementModel(
    val id: String,
    val materialId: String,
    val previousQuantity: Int,
    val newQuantity: Int,
    val reason: String,
    val changedByUsername: String,
    val changedAt: Instant
)
