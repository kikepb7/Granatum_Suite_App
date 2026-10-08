package com.granatum.feature.inventory.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.MaterialCondition
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import com.granatum.feature.inventory.presentation.list.MaterialListAction
import com.granatum.feature.inventory.presentation.list.MaterialListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MaterialListViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeInventoryRepository(
        listOf(
            material("1", "Jarrón alto"),
            material("2", "Cinta roja", RIBBONS, available = 0),
            material("3", "Árbol seco", condition = MaterialCondition.DANADO)
        )
    )

    @Test
    fun refreshes_on_open_and_lists_sorted_by_name() = runTest {
        val vm = MaterialListViewModel(InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertEquals(1, repo.refreshes)
        assertEquals(listOf("Árbol seco", "Cinta roja", "Jarrón alto"), vm.state.value.materials.map { it.name })
        assertFalse(vm.state.value.isStale)
    }

    @Test
    fun filters_combine_search_category_condition_and_out_of_stock() = runTest {
        val vm = MaterialListViewModel(InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onAction(MaterialListAction.OnCategory(VASES.id))
        assertEquals(setOf("1", "3"), vm.state.value.materials.map { it.id }.toSet())
        vm.onAction(MaterialListAction.OnCondition(MaterialCondition.DANADO))
        assertEquals(listOf("3"), vm.state.value.materials.map { it.id })
        vm.onAction(MaterialListAction.OnCondition(null))
        vm.onAction(MaterialListAction.OnCategory(null))
        vm.onAction(MaterialListAction.OnToggleOutOfStock)
        assertEquals(listOf("2"), vm.state.value.materials.map { it.id })
        vm.onAction(MaterialListAction.OnToggleOutOfStock)

        vm.query.setTextAndPlaceCursorAtEnd("arbol")
        Snapshot.sendApplyNotifications()
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("3"), vm.state.value.materials.map { it.id })
        assertFalse(vm.state.value.isEmptyInventory)
    }

    @Test
    fun a_refresh_without_coverage_keeps_the_cache_and_marks_it_stale() = runTest {
        repo.failure = InventoryError.NoInternet
        val vm = MaterialListViewModel(InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertTrue(vm.state.value.isStale)
        assertEquals(3, vm.state.value.materials.size)

        repo.failure = null
        vm.onAction(MaterialListAction.OnRefresh)
        assertFalse(vm.state.value.isStale)
        assertEquals(2, repo.refreshes)
    }
}
