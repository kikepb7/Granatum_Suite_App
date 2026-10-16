package com.granatum.feature.inventory.domain

import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.Dimensions
import com.granatum.feature.inventory.domain.model.ProductFilter
import com.granatum.feature.inventory.domain.model.ProductModel
import com.granatum.feature.inventory.domain.model.ProductSort
import com.granatum.feature.inventory.domain.model.StockFilter
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.model.Warehouse
import com.granatum.feature.inventory.domain.model.filterBy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ProductFilterTest {
    private val vases = CategoryModel("c1", "Jarrones", null)
    private val ribbons = CategoryModel("c2", "Árboles de cinta", null)

    private fun product(id: String, category: CategoryModel, description: String, quantity: Int, location: StorageLocation?, updated: Long) = ProductModel(
        id = id, category = category, description = description, dimensions = Dimensions(10.0, 10.0), quantity = quantity,
        imageUrl = null, location = location, createdAt = Instant.fromEpochSeconds(0), updatedAt = Instant.fromEpochSeconds(updated)
    )

    private val all = listOf(
        product("1", vases, "Cilindro de cristal", 8, StorageLocation(Warehouse.A, 2), updated = 30),
        product("2", vases, "Cerámica blanca", 0, StorageLocation(Warehouse.B, 2), updated = 10),
        product("3", ribbons, "Raso rojo", 30, StorageLocation(Warehouse.B, 4), updated = 20),
        product("4", ribbons, "Arpillera", 12, null, updated = 40)
    )

    private fun ids(filter: ProductFilter) = all.filterBy(filter).map { it.id }

    @Test
    fun the_default_shows_everything_most_recently_changed_first() = assertEquals(listOf("4", "1", "3", "2"), ids(ProductFilter()))

    @Test
    fun search_ignores_case_and_accents_across_category_description_and_location() {
        assertEquals(listOf("2"), ids(ProductFilter(query = "CERAMICA")))
        assertEquals(listOf("4", "3"), ids(ProductFilter(query = "arbol")))
        assertEquals(listOf("3"), ids(ProductFilter(query = "b-4")))
    }

    @Test
    fun warehouse_section_category_and_stock_narrow_together() {
        assertEquals(listOf("3", "2"), ids(ProductFilter(warehouse = Warehouse.B)))
        assertEquals(listOf("1", "2"), ids(ProductFilter(section = 2, sort = ProductSort.QUANTITY_DESC)))
        assertEquals(listOf("2"), ids(ProductFilter(categoryId = "c1", stock = StockFilter.OUT_OF_STOCK)))
        assertEquals(listOf("4", "1", "3"), ids(ProductFilter(stock = StockFilter.IN_STOCK)))
    }

    @Test
    fun sorting_by_category_follows_spanish_order() {
        assertEquals(listOf("4", "3", "2", "1"), ids(ProductFilter(sort = ProductSort.CATEGORY)))
        assertEquals(listOf("2", "1", "4", "3"), ids(ProductFilter(sort = ProductSort.QUANTITY_ASC)))
    }

    @Test
    fun the_button_counts_only_narrowing_filters_and_clearing_keeps_text_and_order() {
        val filter = ProductFilter(query = "x", warehouse = Warehouse.A, section = 1, categoryId = "c1", stock = StockFilter.IN_STOCK, sort = ProductSort.CATEGORY)
        assertEquals(4, filter.activeCount)
        assertEquals(ProductFilter(query = "x", sort = ProductSort.CATEGORY), filter.cleared())
        assertEquals(0, ProductFilter(query = "x", sort = ProductSort.QUANTITY_ASC).activeCount)
    }
}
