package com.granatum.feature.inventory.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import com.granatum.feature.inventory.domain.validation.AdjustmentIssue
import com.granatum.feature.inventory.presentation.detail.MaterialDetailAction
import com.granatum.feature.inventory.presentation.detail.MaterialDetailEvent
import com.granatum.feature.inventory.presentation.detail.MaterialDetailViewModel
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MaterialDetailViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeInventoryRepository(listOf(material("1", "Jarrón", available = 5, total = 10)))

    @Test
    fun the_adjustment_is_validated_before_sending() = runTest {
        val vm = MaterialDetailViewModel("1", InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onAction(MaterialDetailAction.OnAdjust)
        assertEquals("5", vm.newQuantity.text.toString())

        vm.newQuantity.setTextAndPlaceCursorAtEnd("abc")
        vm.onAction(MaterialDetailAction.OnAdjustSave)
        assertTrue(vm.state.value.adjustNotANumber)

        vm.newQuantity.setTextAndPlaceCursorAtEnd("11")
        vm.onAction(MaterialDetailAction.OnAdjustSave)
        assertTrue(AdjustmentIssue.OUT_OF_RANGE in vm.state.value.adjustIssues)
        assertTrue(AdjustmentIssue.REASON_REQUIRED in vm.state.value.adjustIssues)
        assertTrue(repo.adjusted.isEmpty())
    }

    @Test
    fun a_valid_adjustment_is_sent_with_its_reason() = runTest {
        val vm = MaterialDetailViewModel("1", InventoryUseCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onAction(MaterialDetailAction.OnAdjust)
        vm.newQuantity.setTextAndPlaceCursorAtEnd("3")
        vm.reason.setTextAndPlaceCursorAtEnd("Boda del sábado")
        vm.onAction(MaterialDetailAction.OnAdjustSave)

        assertEquals(listOf(Triple("1", 3, "Boda del sábado")), repo.adjusted)
        assertFalse(vm.state.value.isAdjusting)
        assertEquals(3, vm.state.value.material?.available)
    }

    @Test
    fun without_coverage_the_sheet_stays_open_and_says_why() = runTest {
        val vm = MaterialDetailViewModel("1", InventoryUseCases(repo))
        val events = mutableListOf<MaterialDetailEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.eventFlow.collect { events += it } }

        repo.failure = InventoryError.NoInternet
        vm.onAction(MaterialDetailAction.OnAdjust)
        vm.newQuantity.setTextAndPlaceCursorAtEnd("3")
        vm.reason.setTextAndPlaceCursorAtEnd("Uso")
        vm.onAction(MaterialDetailAction.OnAdjustSave)

        assertTrue(vm.state.value.isAdjusting)
        assertIs<MaterialDetailEvent.Message>(events.single())
    }

    @Test
    fun deleting_asks_first_and_then_leaves() = runTest {
        val vm = MaterialDetailViewModel("1", InventoryUseCases(repo))
        val events = mutableListOf<MaterialDetailEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.eventFlow.collect { events += it } }

        vm.onAction(MaterialDetailAction.OnDelete)
        assertTrue(vm.state.value.isConfirmingDelete)
        vm.onAction(MaterialDetailAction.OnDeleteConfirm)

        assertEquals(listOf<MaterialDetailEvent>(MaterialDetailEvent.Deleted), events)
        assertNull(vm.state.value.material)
    }
}
