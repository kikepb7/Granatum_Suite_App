package com.granatum.feature.inventory.domain.model

import com.granatum.core.domain.util.Error
import kotlin.time.Instant

// The inventory products (specs/011-inventario-productos, spec.md FR-001).

data class CategoryModel(val id: String, val name: String, val description: String?)

/** The three warehouses. Named A, B and C for now; each has sections 1 to 5. */
enum class Warehouse { A, B, C }

/** Where a product is: a warehouse and one of its sections (the server's `location`). */
data class StorageLocation(val warehouse: Warehouse, val section: Int) {
    init {
        require(section in SECTIONS) { "Section $section out of $SECTIONS" }
    }

    /** `A-3`: short, and what people type when they search by location. */
    val code: String get() = "${warehouse.name}-$section"

    companion object {
        val SECTIONS = 1..5
    }
}

/** Height and width, in centimetres; the server wants both above zero, with up to two decimals. */
data class Dimensions(val height: Double, val width: Double)

data class ProductModel(
    val id: String,
    /** The category is the product's name (decided 2026-10-17). */
    val category: CategoryModel,
    val description: String,
    val dimensions: Dimensions,
    val quantity: Int,
    /**
     * A temporary signed address the server hands out (backend 010, D-005): it expires, so it is
     * refreshed with the product. Null without image, or when the server could not sign it.
     */
    val imageUrl: String?,
    val location: StorageLocation,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val isOutOfStock: Boolean get() = quantity == 0
}

/**
 * What the form edits. [quantity] is only used when creating: afterwards it changes with history.
 * The image is not part of it: the server takes it apart, once the product exists.
 */
data class ProductDraft(
    val categoryId: String?,
    val description: String,
    val dimensions: Dimensions,
    val quantity: Int,
    val location: StorageLocation?
)

enum class HistoryChangeType { QUANTITY, STATUS, LOCATION, UNIT_PRICE, SUPPLIER, OTHER }

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

    /** Assigned to a scheduled event, so it cannot be deleted (`409 MATERIAL_IN_USE`, backend 011). */
    data object ProductInUse : InventoryError
    data object CategoryNotFound : InventoryError
    data object QuantityOutOfRange : InventoryError
    data object Invalid : InventoryError
    data object Forbidden : InventoryError

    /** The server's image storage is not configured or is down (`503 IMAGE_STORAGE_UNAVAILABLE`). */
    data object StorageUnavailable : InventoryError

    /** Over 10 MB (`413 IMAGE_TOO_LARGE`). */
    data object ImageTooLarge : InventoryError

    /** Not a JPEG, PNG or WebP by its bytes, or empty (`415 INVALID_IMAGE`). */
    data object ImageInvalid : InventoryError
    data object Unknown : InventoryError
}
