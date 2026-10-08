package com.granatum.feature.inventory.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import com.granatum.feature.inventory.domain.validation.CategoryIssue
import com.granatum.feature.inventory.presentation.category.CategoryAction
import com.granatum.feature.inventory.presentation.category.CategoryListViewModel
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryListViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeInventoryRepository(listOf(material("1", "Jarrón", VASES)))

    @Test
    fun a_category_in_use_cannot_be_deleted() = runTest {
        val vm = CategoryListViewModel(InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        val (vases, ribbons) = vm.state.value.rows
        assertEquals(1, vases.materialCount)
        assertFalse(vases.canDelete)
        assertTrue(ribbons.canDelete)

        vm.onAction(CategoryAction.OnDelete(VASES))
        assertNull(vm.state.value.deleting)

        vm.onAction(CategoryAction.OnDelete(RIBBONS))
        vm.onAction(CategoryAction.OnConfirmDelete)
        assertEquals(listOf(RIBBONS.id), repo.deletedCategories)
        assertEquals(1, vm.state.value.rows.size)
    }

    @Test
    fun creating_validates_and_then_saves() = runTest {
        val vm = CategoryListViewModel(InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onAction(CategoryAction.OnNew)
        assertEquals("", vm.state.value.editingId)
        vm.onAction(CategoryAction.OnSave)
        assertTrue(CategoryIssue.NAME_REQUIRED in vm.state.value.issues)

        vm.name.setTextAndPlaceCursorAtEnd("Velas")
        vm.onAction(CategoryAction.OnSave)
        assertNull(vm.state.value.editingId)
        assertTrue(vm.state.value.rows.any { it.category.name == "Velas" && it.category.description == null })
    }

    @Test
    fun renaming_prefills_and_refreshes_the_materials() = runTest {
        val vm = CategoryListViewModel(InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onAction(CategoryAction.OnEdit(RIBBONS))
        assertEquals("De raso", vm.description.text.toString())
        vm.name.setTextAndPlaceCursorAtEnd("Lazos")
        vm.onAction(CategoryAction.OnSave)

        assertEquals("Lazos", vm.state.value.rows.first { it.category.id == RIBBONS.id }.category.name)
        assertEquals(1, repo.refreshes)
    }

    @Test
    fun without_coverage_the_editor_stays_open_with_the_error() = runTest {
        val vm = CategoryListViewModel(InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        repo.failure = InventoryError.NoInternet

        vm.onAction(CategoryAction.OnNew)
        vm.name.setTextAndPlaceCursorAtEnd("Velas")
        vm.onAction(CategoryAction.OnSave)

        assertEquals("", vm.state.value.editingId)
        assertNotNull(vm.state.value.error)
    }
}
