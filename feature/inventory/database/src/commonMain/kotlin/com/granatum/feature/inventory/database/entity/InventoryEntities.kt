package com.granatum.feature.inventory.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// A read cache of the server's inventory (specs/006-inventario-real, specs/011-inventario-productos): nothing
// here is waiting to be sent, so the schema can be rebuilt from the server at any time.

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?
)

/** The server's material (backend 010). [imageUrl] is a signed URL that expires: refreshed with it. */
@Entity(tableName = "material", indices = [Index("categoryId")])
data class MaterialEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val description: String,
    val height: Double,
    val width: Double,
    val quantity: Int,
    val warehouse: String,
    val section: Int,
    val imageUrl: String?,
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
