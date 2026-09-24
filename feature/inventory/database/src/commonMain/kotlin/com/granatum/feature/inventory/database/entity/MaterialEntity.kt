package com.granatum.feature.inventory.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "material")
data class MaterialEntity(
    @PrimaryKey val materialId: String,
    val name: String,
    val category: String,
    val status: String,
    val quantity: Int,
    val size: String?,
    val color: String?,
    val location: String?,
    val photoUrlsCsv: String,
    val notes: String?,
    val updatedAtEpochMillis: Long
)
