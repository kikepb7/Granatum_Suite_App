package com.granatum.feature.inventory.domain.validation

import com.granatum.feature.inventory.domain.model.MaterialDraft

enum class DraftIssue {
    NAME_REQUIRED, NAME_TOO_LONG, CATEGORY_REQUIRED, COLOR_REQUIRED, MATERIAL_REQUIRED,
    LOCATION_REQUIRED, SUPPLIER_REQUIRED, NEGATIVE_QUANTITY, AVAILABLE_OVER_TOTAL,
    NEGATIVE_PRICE, NEGATIVE_SIZE
}

enum class AdjustmentIssue { OUT_OF_RANGE, UNCHANGED, REASON_REQUIRED }

enum class CategoryIssue { NAME_REQUIRED, NAME_TOO_LONG, DESCRIPTION_TOO_LONG }

/** The server's limits (docs/openapi.json), checked before sending (FR-006, FR-009, FR-012). */
object InventoryValidation {
    const val NAME_MAX = 140
    const val CATEGORY_NAME_MAX = 100
    const val CATEGORY_DESCRIPTION_MAX = 500

    fun validate(d: MaterialDraft): Set<DraftIssue> = buildSet {
        if (d.name.isBlank()) add(DraftIssue.NAME_REQUIRED)
        if (d.name.trim().length > NAME_MAX) add(DraftIssue.NAME_TOO_LONG)
        if (d.categoryId == null) add(DraftIssue.CATEGORY_REQUIRED)
        if (d.color.isBlank()) add(DraftIssue.COLOR_REQUIRED)
        if (d.physicalMaterial.isBlank()) add(DraftIssue.MATERIAL_REQUIRED)
        if (d.location.isBlank()) add(DraftIssue.LOCATION_REQUIRED)
        if (d.supplier.isBlank()) add(DraftIssue.SUPPLIER_REQUIRED)
        if (d.available < 0 || d.total < 0) add(DraftIssue.NEGATIVE_QUANTITY)
        if (d.available > d.total) add(DraftIssue.AVAILABLE_OVER_TOTAL)
        if (d.unitPrice < 0) add(DraftIssue.NEGATIVE_PRICE)
        if (d.size.height < 0 || d.size.width < 0 || (d.size.diameter ?: 0.0) < 0) add(DraftIssue.NEGATIVE_SIZE)
    }

    fun validateAdjustment(current: Int, total: Int, newValue: Int, reason: String): Set<AdjustmentIssue> = buildSet {
        if (newValue < 0 || newValue > total) add(AdjustmentIssue.OUT_OF_RANGE)
        if (newValue == current) add(AdjustmentIssue.UNCHANGED)
        if (reason.isBlank()) add(AdjustmentIssue.REASON_REQUIRED)
    }

    fun validateCategory(name: String, description: String?): Set<CategoryIssue> = buildSet {
        if (name.isBlank()) add(CategoryIssue.NAME_REQUIRED)
        if (name.trim().length > CATEGORY_NAME_MAX) add(CategoryIssue.NAME_TOO_LONG)
        if ((description?.length ?: 0) > CATEGORY_DESCRIPTION_MAX) add(CategoryIssue.DESCRIPTION_TOO_LONG)
    }
}
