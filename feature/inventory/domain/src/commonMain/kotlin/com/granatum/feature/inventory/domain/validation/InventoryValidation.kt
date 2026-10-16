package com.granatum.feature.inventory.domain.validation

import com.granatum.feature.inventory.domain.model.ProductDraft

enum class DraftIssue { CATEGORY_REQUIRED, DESCRIPTION_REQUIRED, DESCRIPTION_TOO_LONG, NEGATIVE_SIZE, NEGATIVE_QUANTITY, LOCATION_REQUIRED }

enum class QuantityIssue { NEGATIVE, UNCHANGED, REASON_TOO_LONG }

enum class CategoryIssue { NAME_REQUIRED, NAME_TOO_LONG, DESCRIPTION_TOO_LONG }

/** The server's limits (docs/openapi.json), checked before sending (specs/011, FR-001). */
object InventoryValidation {
    /** The description travels as the server's `nombre` (research D1). */
    const val DESCRIPTION_MAX = 140
    const val REASON_MAX = 500
    const val CATEGORY_NAME_MAX = 100
    const val CATEGORY_DESCRIPTION_MAX = 500
    const val IMAGE_MAX_BYTES = 10L * 1024 * 1024

    fun validate(d: ProductDraft): Set<DraftIssue> = buildSet {
        if (d.categoryId == null) add(DraftIssue.CATEGORY_REQUIRED)
        if (d.description.isBlank()) add(DraftIssue.DESCRIPTION_REQUIRED)
        if (d.description.trim().length > DESCRIPTION_MAX) add(DraftIssue.DESCRIPTION_TOO_LONG)
        if (d.dimensions.height < 0 || d.dimensions.width < 0) add(DraftIssue.NEGATIVE_SIZE)
        if (d.quantity < 0) add(DraftIssue.NEGATIVE_QUANTITY)
        if (d.location == null) add(DraftIssue.LOCATION_REQUIRED)
    }

    fun validateQuantity(current: Int, newValue: Int, reason: String?): Set<QuantityIssue> = buildSet {
        if (newValue < 0) add(QuantityIssue.NEGATIVE)
        if (newValue == current) add(QuantityIssue.UNCHANGED)
        if ((reason?.trim()?.length ?: 0) > REASON_MAX) add(QuantityIssue.REASON_TOO_LONG)
    }

    fun validateCategory(name: String, description: String?): Set<CategoryIssue> = buildSet {
        if (name.isBlank()) add(CategoryIssue.NAME_REQUIRED)
        if (name.trim().length > CATEGORY_NAME_MAX) add(CategoryIssue.NAME_TOO_LONG)
        if ((description?.length ?: 0) > CATEGORY_DESCRIPTION_MAX) add(CategoryIssue.DESCRIPTION_TOO_LONG)
    }
}
