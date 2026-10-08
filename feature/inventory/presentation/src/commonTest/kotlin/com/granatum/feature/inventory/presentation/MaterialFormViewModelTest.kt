package com.granatum.feature.inventory.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.SizeUnit
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import com.granatum.feature.inventory.domain.validation.DraftIssue
import com.granatum.feature.inventory.presentation.form.MaterialFormAction
import com.granatum.feature.inventory.presentation.form.MaterialFormEvent
import com.granatum.feature.inventory.presentation.form.MaterialFormViewModel
import com.granatum.feature.inventory.presentation.form.NumberField
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MaterialFormViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeInventoryRepository(listOf(material("1", "Jarrón")))

    private fun MaterialFormViewModel.fill() {
        name.setTextAndPlaceCursorAtEnd("Cesta")
        available.setTextAndPlaceCursorAtEnd("2")
        total.setTextAndPlaceCursorAtEnd("4")
        height.setTextAndPlaceCursorAtEnd("30")
        width.setTextAndPlaceCursorAtEnd("25,5")
        color.setTextAndPlaceCursorAtEnd("Natural")
        physicalMaterial.setTextAndPlaceCursorAtEnd("Mimbre")
        location.setTextAndPlaceCursorAtEnd("Estante 2")
        price.setTextAndPlaceCursorAtEnd("7,90")
        supplier.setTextAndPlaceCursorAtEnd("Cestería Sur")
        onAction(MaterialFormAction.OnCategory(RIBBONS.id))
    }

    @Test
    fun an_empty_form_is_flagged_field_by_field_and_not_sent() = runTest {
        val vm = MaterialFormViewModel(null, InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onAction(MaterialFormAction.OnSave)

        val state = vm.state.value
        assertTrue(DraftIssue.NAME_REQUIRED in state.issues)
        assertTrue(DraftIssue.CATEGORY_REQUIRED in state.issues)
        assertTrue(NumberField.HEIGHT in state.badNumbers)
        assertTrue(NumberField.PRICE in state.badNumbers)
        assertTrue(repo.created.isEmpty())
    }

    @Test
    fun a_complete_form_creates_the_material_with_decimal_commas() = runTest {
        val vm = MaterialFormViewModel(null, InventoryUseCases(repo))
        val events = mutableListOf<MaterialFormEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.eventFlow.collect { events += it } }

        vm.fill()
        vm.onAction(MaterialFormAction.OnUnit(SizeUnit.MM))
        vm.onAction(MaterialFormAction.OnSave)

        val draft = repo.created.single()
        assertEquals(RIBBONS.id, draft.categoryId)
        assertEquals(25.5, draft.size.width)
        assertEquals(null, draft.size.diameter)
        assertEquals(SizeUnit.MM, draft.size.unit)
        assertEquals(7.9, draft.unitPrice)
        assertEquals(listOf<MaterialFormEvent>(MaterialFormEvent.Saved("new")), events)
    }

    @Test
    fun editing_prefills_and_keeps_the_photos() = runTest {
        val vm = MaterialFormViewModel("1", InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertTrue(vm.state.value.ready)
        assertEquals("Jarrón", vm.name.text.toString())
        vm.name.setTextAndPlaceCursorAtEnd("Jarrón grande")
        vm.onAction(MaterialFormAction.OnSave)

        val (id, draft) = repo.updated.single()
        assertEquals("1", id)
        assertEquals("Jarrón grande", draft.name)
        assertEquals(listOf("https://example.test/a.jpg"), draft.photos)
    }

    @Test
    fun a_server_error_is_shown_and_nothing_is_lost() = runTest {
        val vm = MaterialFormViewModel(null, InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        repo.failure = InventoryError.CategoryNotFound

        vm.fill()
        vm.onAction(MaterialFormAction.OnSave)

        assertNotNull(vm.state.value.error)
        assertEquals("Cesta", vm.name.text.toString())
    }
}
