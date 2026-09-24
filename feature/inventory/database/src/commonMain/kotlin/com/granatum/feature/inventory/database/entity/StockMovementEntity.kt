package com.granatum.feature.inventory.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_movement")
data class StockMovementEntity(
    @PrimaryKey val movementId: String,
    val materialId: String,
    val previousQuantity: Int,
    val newQuantity: Int,
    val reason: String,
    val changedByUsername: String,
    val changedAtEpochMillis: Long
)
