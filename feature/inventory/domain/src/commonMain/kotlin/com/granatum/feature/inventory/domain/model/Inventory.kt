package com.granatum.feature.inventory.domain.model

import com.granatum.core.domain.util.Error
import kotlin.time.Instant

// The inventory products (specs/011-inventario-productos, spec.md FR-001).

data class CategoryModel(val id: String, val name: String, val description: String?)

/** The three warehouses. Named A, B and C for now; each has sections 1 to 5. */
enum class Warehouse { A, B, C }

/** Where a product is: a warehouse and one of its sections. */
data class StorageLocation(val warehouse: Warehouse, val section: Int) {
    init {
        require(section in SECTIONS) { "Section $section out of $SECTIONS" }
    }

    /** How the server keeps it in `ubicacion` (research D1): `A-3`. */
    fun toApi(): String = "${warehouse.name}-$section"

    companion object {
        val SECTIONS = 1..5

        /** Null for anything that is not `A-3` and the like, e.g. free text saved before. */
        fun parse(text: String?): StorageLocation? {
            val match = Regex("""^\s*([ABCabc])\s*[-·/ ]?\s*([1-5])\s*$""").matchEntire(text.orEmpty()) ?: return null
            val (warehouse, section) = match.destructured
            return StorageLocation(Warehouse.valueOf(warehouse.uppercase()), section.toInt())
        }
    }
}

/** Height and width, in centimetres. */
data class Dimensions(val height: Double, val width: Double)

data class ProductModel(
    val id: String,
    /** The category is the product's name (decided 2026-10-17). */
    val category: CategoryModel,
    val description: String,
    val dimensions: Dimensions,
    val quantity: Int,
    /** Public address in the image storage; null without image. */
    val imageUrl: String?,
    /** Null for products saved before with a location that is not a warehouse and section. */
    val location: StorageLocation?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val isOutOfStock: Boolean get() = quantity == 0
}

/** What the form edits. [quantity] is only used when creating: afterwards it changes with history. */
data class ProductDraft(
    val categoryId: String?,
    val description: String,
    val dimensions: Dimensions,
    val quantity: Int,
    val imageUrl: String?,
    val location: StorageLocation?
)

enum class HistoryChangeType { CANTIDAD, ESTADO, UBICACION, PRECIO_UNITARIO, PROVEEDOR, OTRO }

data class ProductHistoryEntry(
    val id: String,
    val type: HistoryChangeType,
    val previousValue: String?,
    val newValue: String?,
    val reason: String,
    val at: Instant
)

sealed interface InventoryError : Error {
    /** Every change needs a connection: nothing is queued for later. */
    data object NoInternet : InventoryError
    data object ProductNotFound : InventoryError
    data object CategoryNotFound : InventoryError
    data object QuantityOutOfRange : InventoryError
    data object Invalid : InventoryError
    data object Forbidden : InventoryError

    /** This environment has no image storage configured. */
    data object StorageUnavailable : InventoryError
    data object ImageTooLarge : InventoryError
    data object ImageUploadFailed : InventoryError
    data object Unknown : InventoryError
}
