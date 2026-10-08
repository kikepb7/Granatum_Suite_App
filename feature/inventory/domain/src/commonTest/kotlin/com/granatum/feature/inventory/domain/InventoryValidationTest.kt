package com.granatum.feature.inventory.domain

import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.MaterialCondition
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialFilter
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.MaterialSize
import com.granatum.feature.inventory.domain.model.SizeUnit
import com.granatum.feature.inventory.domain.model.filterBy
import com.granatum.feature.inventory.domain.validation.AdjustmentIssue
import com.granatum.feature.inventory.domain.validation.CategoryIssue
import com.granatum.feature.inventory.domain.validation.DraftIssue
import com.granatum.feature.inventory.domain.validation.InventoryValidation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class InventoryValidationTest {
    private val size = MaterialSize(40.0, 15.0, null, SizeUnit.CM)
    private fun draft(
        name: String = "Cilindro", categoryId: String? = "c1", available: Int = 5, total: Int = 10,
        color: String = "Transparente", price: Double = 12.5, size: MaterialSize = this.size
    ) = MaterialDraft(name, categoryId, available, total, size, color, "Cristal", MaterialCondition.NUEVO, "Almacén A", price, "Cristalería Sur", emptyList())

    @Test
    fun a_complete_draft_is_valid() = assertTrue(InventoryValidation.validate(draft()).isEmpty())

    @Test
    fun each_rule_on_its_own() {
        assertEquals(setOf(DraftIssue.NAME_REQUIRED), InventoryValidation.validate(draft(name = " ")))
        assertEquals(setOf(DraftIssue.NAME_TOO_LONG), InventoryValidation.validate(draft(name = "x".repeat(141))))
        assertTrue(InventoryValidation.validate(draft(name = "x".repeat(140))).isEmpty())
        assertEquals(setOf(DraftIssue.CATEGORY_REQUIRED), InventoryValidation.validate(draft(categoryId = null)))
        assertEquals(setOf(DraftIssue.COLOR_REQUIRED), InventoryValidation.validate(draft(color = "")))
        assertEquals(setOf(DraftIssue.AVAILABLE_OVER_TOTAL), InventoryValidation.validate(draft(available = 11)))
        assertEquals(setOf(DraftIssue.NEGATIVE_QUANTITY, DraftIssue.AVAILABLE_OVER_TOTAL), InventoryValidation.validate(draft(total = -1)))
        assertEquals(setOf(DraftIssue.NEGATIVE_PRICE), InventoryValidation.validate(draft(price = -0.01)))
        assertEquals(setOf(DraftIssue.NEGATIVE_SIZE), InventoryValidation.validate(draft(size = size.copy(diameter = -1.0))))
    }

    @Test
    fun adjustments_stay_within_zero_and_total_and_need_a_reason() {
        assertTrue(InventoryValidation.validateAdjustment(5, 10, 10, "Recibido").isEmpty())
        assertEquals(setOf(AdjustmentIssue.OUT_OF_RANGE), InventoryValidation.validateAdjustment(5, 10, 11, "x"))
        assertEquals(setOf(AdjustmentIssue.OUT_OF_RANGE), InventoryValidation.validateAdjustment(5, 10, -1, "x"))
        assertEquals(setOf(AdjustmentIssue.UNCHANGED, AdjustmentIssue.REASON_REQUIRED), InventoryValidation.validateAdjustment(5, 10, 5, " "))
    }

    @Test
    fun category_limits() {
        assertTrue(InventoryValidation.validateCategory("Cilindros", null).isEmpty())
        assertEquals(setOf(CategoryIssue.NAME_REQUIRED), InventoryValidation.validateCategory("", null))
        assertEquals(setOf(CategoryIssue.NAME_TOO_LONG), InventoryValidation.validateCategory("x".repeat(101), null))
        assertEquals(setOf(CategoryIssue.DESCRIPTION_TOO_LONG), InventoryValidation.validateCategory("ok", "x".repeat(501)))
    }

    @Test
    fun the_filter_matches_text_category_condition_and_out_of_stock() {
        val t = Instant.parse("2026-10-08T00:00:00Z")
        fun m(id: String, name: String, cat: String, cond: MaterialCondition, available: Int, location: String = "A") =
            MaterialModel(id, name, CategoryModel(cat, "Cat $cat", null), available, 10, size, "c", "m", cond, location, 1.0, "S", emptyList(), t, t)
        val all = listOf(m("1", "Jarrón", "c1", MaterialCondition.NUEVO, 3), m("2", "Cinta", "c2", MaterialCondition.DANADO, 0, location = "Almacén B"), m("3", "árbol", "c1", MaterialCondition.USADO, 1))
        assertEquals(listOf("3", "2", "1"), all.filterBy(MaterialFilter()).map { it.id }, "sorted by name, case-insensitive")
        assertEquals(listOf("2"), all.filterBy(MaterialFilter(query = "almacen b")).map { it.id }, "accents ignored")
        assertEquals(listOf("3", "1"), all.filterBy(MaterialFilter(categoryId = "c1")).map { it.id })
        assertEquals(listOf("2"), all.filterBy(MaterialFilter(condition = MaterialCondition.DANADO)).map { it.id })
        assertEquals(listOf("2"), all.filterBy(MaterialFilter(onlyOutOfStock = true)).map { it.id })
    }
}
