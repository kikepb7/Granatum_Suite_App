package com.granatum.feature.inventory.domain

import com.granatum.feature.inventory.domain.model.Dimensions
import com.granatum.feature.inventory.domain.model.ProductDraft
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.model.Warehouse
import com.granatum.feature.inventory.domain.validation.CategoryIssue
import com.granatum.feature.inventory.domain.validation.DraftIssue
import com.granatum.feature.inventory.domain.validation.InventoryValidation
import com.granatum.feature.inventory.domain.validation.QuantityIssue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InventoryValidationTest {
    private fun draft(
        categoryId: String? = "c1",
        description: String = "Jarrón cilíndrico",
        dimensions: Dimensions = Dimensions(40.0, 12.0),
        quantity: Int = 5,
        location: StorageLocation? = StorageLocation(Warehouse.A, 2)
    ) = ProductDraft(categoryId, description, dimensions, quantity, location = location)

    @Test
    fun a_complete_draft_is_valid() = assertTrue(InventoryValidation.validate(draft()).isEmpty())

    @Test
    fun category_description_and_location_are_required() {
        val issues = InventoryValidation.validate(draft(categoryId = null, description = "  ", location = null))
        assertEquals(setOf(DraftIssue.CATEGORY_REQUIRED, DraftIssue.DESCRIPTION_REQUIRED, DraftIssue.LOCATION_REQUIRED), issues)
    }

    @Test
    fun the_description_fits_the_server_limit() {
        assertTrue(InventoryValidation.validate(draft(description = "a".repeat(500))).isEmpty())
        assertEquals(setOf(DraftIssue.DESCRIPTION_TOO_LONG), InventoryValidation.validate(draft(description = "a".repeat(501))))
    }

    @Test
    fun sizes_must_be_above_zero_and_quantity_not_negative() {
        assertEquals(setOf(DraftIssue.INVALID_SIZE, DraftIssue.NEGATIVE_QUANTITY), InventoryValidation.validate(draft(dimensions = Dimensions(-1.0, 3.0), quantity = -2)))
        assertEquals(setOf(DraftIssue.INVALID_SIZE), InventoryValidation.validate(draft(dimensions = Dimensions(10.0, 0.0))))
        assertTrue(InventoryValidation.validate(draft(quantity = 0)).isEmpty())
    }

    @Test
    fun a_quantity_change_must_change_something_and_keep_the_reason_short() {
        assertTrue(InventoryValidation.validateQuantity(5, 8, null).isEmpty())
        assertEquals(setOf(QuantityIssue.UNCHANGED), InventoryValidation.validateQuantity(5, 5, "x"))
        assertEquals(setOf(QuantityIssue.NEGATIVE), InventoryValidation.validateQuantity(5, -1, null))
        assertEquals(setOf(QuantityIssue.REASON_TOO_LONG), InventoryValidation.validateQuantity(5, 4, "a".repeat(501)))
    }

    @Test
    fun categories_have_a_name_within_limits() {
        assertEquals(setOf(CategoryIssue.NAME_REQUIRED), InventoryValidation.validateCategory(" ", null))
        assertEquals(setOf(CategoryIssue.NAME_TOO_LONG), InventoryValidation.validateCategory("a".repeat(101), null))
        assertEquals(setOf(CategoryIssue.DESCRIPTION_TOO_LONG), InventoryValidation.validateCategory("Jarrones", "a".repeat(501)))
    }

    @Test
    fun a_location_reads_as_its_code() = assertEquals("C-5", StorageLocation(Warehouse.C, 5).code)

    @Test
    fun sections_go_from_one_to_five() {
        assertFailsWith<IllegalArgumentException> { StorageLocation(Warehouse.A, 6) }
    }
}
