package com.granatum.feature.inventory.domain.model

import com.granatum.core.domain.util.Error
import kotlin.time.Instant

// The inventory as the server stores it (specs/006-inventario-real, data-model.md).

data class CategoryModel(val id: String, val name: String, val description: String?)

/** The physical condition of a material. Stock levels are the available/total quantities. */
enum class MaterialCondition { NUEVO, USADO, DANADO, ROTO, EN_REPARACION }

enum class SizeUnit { MM, CM, M, IN }

data class MaterialSize(val height: Double, val width: Double, val diameter: Double?, val unit: SizeUnit)

data class MaterialModel(
    val id: String,
    val name: String,
    val category: CategoryModel,
    val available: Int,
    val total: Int,
    val size: MaterialSize,
    val color: String,
    val physicalMaterial: String,
    val condition: MaterialCondition,
    val location: String,
    val unitPrice: Double,
    val supplier: String,
    /** URLs: the server stores them but offers no upload, so they are shown and kept, not added. */
    val photos: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val isOutOfStock: Boolean get() = available == 0
}

/**
 * What the form edits. [available] is only set when creating; afterwards it changes through an
 * adjustment with a reason, so the history records why.
 */
data class MaterialDraft(
    val name: String,
    val categoryId: String?,
    val available: Int,
    val total: Int,
    val size: MaterialSize,
    val color: String,
    val physicalMaterial: String,
    val condition: MaterialCondition,
    val location: String,
    val unitPrice: Double,
    val supplier: String,
    val photos: List<String>
)

enum class HistoryChangeType { CANTIDAD, ESTADO, UBICACION, PRECIO_UNITARIO, PROVEEDOR, OTRO }

data class MaterialHistoryEntry(
    val id: String,
    val type: HistoryChangeType,
    val previousValue: String?,
    val newValue: String?,
    val reason: String,
    val at: Instant
)

sealed interface InventoryError : Error {
    /** Every change needs a connection (FR-014): nothing is queued for later. */
    data object NoInternet : InventoryError
    data object MaterialNotFound : InventoryError
    data object CategoryNotFound : InventoryError
    data object QuantityOutOfRange : InventoryError
    data object Invalid : InventoryError
    data object Forbidden : InventoryError
    data object Unknown : InventoryError
}
