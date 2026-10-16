package com.granatum.feature.inventory.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.StockFilter
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.model.Warehouse
import com.granatum.feature.inventory.presentation.list.FilterPill
import com.granatum.feature.inventory.presentation.list.ProductListAction
import com.granatum.feature.inventory.presentation.list.ProductListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeInventoryRepository(
        listOf(
            product("1", "Jarrón alto", quantity = 8, location = StorageLocation(Warehouse.A, 2), updated = 3),
            product("2", "Cinta roja", RIBBONS, quantity = 0, location = StorageLocation(Warehouse.B, 2), updated = 2),
            product("3", "Árbol seco", quantity = 4, location = StorageLocation(Warehouse.B, 5), updated = 1)
        )
    )

    private fun TestScope.open(): ProductListViewModel {
        val sut = ProductListViewModel(useCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.state.collect {} }
        return sut
    }

    private fun ProductListViewModel.ids() = state.value.products.map { it.id }

    @Test
    fun refreshes_on_open_and_summarises_what_is_shown() = runTest {
        val sut = open()
        assertEquals(1, repo.refreshes)
        assertEquals(listOf("1", "2", "3"), sut.ids())
        assertEquals(12 to 1, sut.state.value.totalUnits to sut.state.value.outOfStock)
        assertEquals(mapOf(StorageLocation(Warehouse.A, 2) to 1, StorageLocation(Warehouse.B, 2) to 1, StorageLocation(Warehouse.B, 5) to 1), sut.state.value.locationCounts)
    }

    @Test
    fun the_warehouse_selector_filters_at_once() = runTest {
        val sut = open()
        sut.onAction(ProductListAction.OnWarehouse(Warehouse.B))
        assertEquals(listOf("2", "3"), sut.ids())
        assertEquals(4 to 1, sut.state.value.totalUnits to sut.state.value.outOfStock)
        sut.onAction(ProductListAction.OnWarehouse(null))
        assertEquals(3, sut.ids().size)
    }

    @Test
    fun the_sheet_edits_a_draft_and_says_how_many_remain_before_applying() = runTest {
        val sut = open()
        sut.onAction(ProductListAction.OnOpenFilters)
        val draft = sut.state.value.sheet!!.draft
        sut.onAction(ProductListAction.OnDraftChange(draft.copy(stock = StockFilter.IN_STOCK, section = 2)))
        assertEquals(1, sut.state.value.sheet!!.matching)
        assertEquals(3, sut.ids().size, "the list does not change until applied")

        sut.onAction(ProductListAction.OnApplyFilters)
        assertNull(sut.state.value.sheet)
        assertEquals(listOf("1"), sut.ids())
        assertEquals(2, sut.state.value.filter.activeCount)
    }

    @Test
    fun dismissing_the_sheet_discards_the_draft() = runTest {
        val sut = open()
        sut.onAction(ProductListAction.OnOpenFilters)
        sut.onAction(ProductListAction.OnDraftChange(sut.state.value.sheet!!.draft.copy(categoryId = RIBBONS.id)))
        sut.onAction(ProductListAction.OnDismissFilters)
        assertEquals(3, sut.ids().size)
    }

    @Test
    fun pills_remove_one_filter_each_and_clearing_keeps_the_search() = runTest {
        val sut = open()
        sut.onAction(ProductListAction.OnOpenFilters)
        sut.onAction(ProductListAction.OnDraftChange(sut.state.value.sheet!!.draft.copy(categoryId = VASES.id, stock = StockFilter.IN_STOCK)))
        sut.onAction(ProductListAction.OnApplyFilters)
        assertEquals(listOf("1", "3"), sut.ids())

        sut.onAction(ProductListAction.OnRemove(FilterPill.CATEGORY))
        assertEquals(listOf("1", "3"), sut.ids())
        assertNull(sut.state.value.filter.categoryId)

        Snapshot.withMutableSnapshot { sut.query.setTextAndPlaceCursorAtEnd("arbol") }
        testScheduler.advanceUntilIdle()
        sut.onAction(ProductListAction.OnClearFilters)
        assertEquals(StockFilter.ALL, sut.state.value.filter.stock)
        assertEquals(listOf("3"), sut.ids())
    }

    @Test
    fun an_empty_inventory_is_told_apart_from_an_empty_result() = runTest {
        val sut = open()
        sut.onAction(ProductListAction.OnWarehouse(Warehouse.C))
        assertTrue(sut.state.value.products.isEmpty())
        assertFalse(sut.state.value.isEmptyInventory)
    }

    @Test
    fun a_refresh_without_coverage_marks_the_list_as_stale() = runTest {
        repo.failure = InventoryError.NoInternet
        val sut = open()
        assertTrue(sut.state.value.isStale)
        repo.failure = null
        sut.onAction(ProductListAction.OnRefresh)
        assertFalse(sut.state.value.isStale)
    }
}
