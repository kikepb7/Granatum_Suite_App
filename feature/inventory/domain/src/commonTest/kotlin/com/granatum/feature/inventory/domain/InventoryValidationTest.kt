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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InventoryValidationTest {
    private fun draft(
        categoryId: String? = "c1",
        description: String = "Jarrón cilíndrico",
        dimensions: Dimensions = Dimensions(40.0, 12.0),
        quantity: Int = 5,
        location: StorageLocation? = StorageLocation(Warehouse.A, 2)
    ) = ProductDraft(categoryId, description, dimensions, quantity, imageUrl = null, location = location)

    @Test
    fun a_complete_draft_is_valid() = assertTrue(InventoryValidation.validate(draft()).isEmpty())

    @Test
    fun category_description_and_location_are_required() {
        val issues = InventoryValidation.validate(draft(categoryId = null, description = "  ", location = null))
        assertEquals(setOf(DraftIssue.CATEGORY_REQUIRED, DraftIssue.DESCRIPTION_REQUIRED, DraftIssue.LOCATION_REQUIRED), issues)
    }

    @Test
    fun the_description_fits_the_servers_name() {
        assertTrue(InventoryValidation.validate(draft(description = "a".repeat(140))).isEmpty())
        assertEquals(setOf(DraftIssue.DESCRIPTION_TOO_LONG), InventoryValidation.validate(draft(description = "a".repeat(141))))
    }

    @Test
    fun sizes_and_quantity_cannot_be_negative() {
        val issues = InventoryValidation.validate(draft(dimensions = Dimensions(-1.0, 3.0), quantity = -2))
        assertEquals(setOf(DraftIssue.NEGATIVE_SIZE, DraftIssue.NEGATIVE_QUANTITY), issues)
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
    fun a_location_travels_as_warehouse_dash_section_and_is_read_back_leniently() {
        assertEquals("C-5", StorageLocation(Warehouse.C, 5).toApi())
        listOf("A-3", "a3", " B · 1 ", "C/4", "b 2").forEach { text -> assertTrue(StorageLocation.parse(text) != null, text) }
        assertEquals(StorageLocation(Warehouse.B, 1), StorageLocation.parse(" B · 1 "))
    }

    @Test
    fun free_text_locations_from_before_are_not_guessed() {
        listOf(null, "", "Almacén A · estante 2", "D-1", "A-6", "A-0").forEach { assertNull(StorageLocation.parse(it), it) }
    }

    @Test
    fun sections_go_from_one_to_five() {
        assertFailsWith<IllegalArgumentException> { StorageLocation(Warehouse.A, 6) }
    }
}
