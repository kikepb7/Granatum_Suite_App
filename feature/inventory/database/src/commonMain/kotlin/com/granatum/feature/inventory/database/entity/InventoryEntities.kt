package com.granatum.feature.inventory.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// A read cache of the server's inventory (specs/006-inventario-real, research D1-D2): nothing
// here is waiting to be sent, so the schema can be rebuilt from the server at any time.

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?
)

@Entity(tableName = "material", indices = [Index("categoryId")])
data class MaterialEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String,
    val available: Int,
    val total: Int,
    val height: Double,
    val width: Double,
    val diameter: Double?,
    val unit: String,
    val color: String,
    val physicalMaterial: String,
    val condition: String,
    val location: String,
    val unitPrice: Double,
    val supplier: String,
    val photosJson: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

@Entity(tableName = "material_history", indices = [Index("materialId")])
data class MaterialHistoryEntity(
    @PrimaryKey val id: String,
    val materialId: String,
    val type: String,
    val previousValue: String?,
    val newValue: String?,
    val reason: String,
    val atEpochMillis: Long
)
