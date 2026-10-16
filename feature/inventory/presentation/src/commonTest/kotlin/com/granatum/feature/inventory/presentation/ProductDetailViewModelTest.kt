package com.granatum.feature.inventory.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.validation.QuantityIssue
import com.granatum.feature.inventory.presentation.detail.ProductDetailAction
import com.granatum.feature.inventory.presentation.detail.ProductDetailEvent
import com.granatum.feature.inventory.presentation.detail.ProductDetailViewModel
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeInventoryRepository(listOf(product("1", "Jarrón alto", quantity = 8)))
    private val events = mutableListOf<ProductDetailEvent>()

    private fun TestScope.open(): ProductDetailViewModel {
        val sut = ProductDetailViewModel("1", useCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.eventFlow.collect { events += it } }
        return sut
    }

    @Test
    fun the_stepper_prepares_a_quantity_that_is_only_sent_on_save() = runTest {
        val sut = open()
        sut.onAction(ProductDetailAction.OnQuantityChange(9))
        sut.onAction(ProductDetailAction.OnQuantityChange(sut.state.value.shownQuantity + 5))
        assertEquals(14, sut.state.value.shownQuantity)
        assertTrue(sut.state.value.hasPendingQuantity)
        assertTrue(repo.quantities.isEmpty())

        sut.reason.setTextAndPlaceCursorAtEnd("  Pedido recibido ")
        sut.onAction(ProductDetailAction.OnQuantitySave)

        assertEquals(Triple("1", 14, "Pedido recibido"), repo.quantities.single())
        assertEquals(14, sut.state.value.product?.quantity)
        assertFalse(sut.state.value.hasPendingQuantity)
        assertEquals("", sut.reason.text.toString())
        assertIs<ProductDetailEvent.Message>(events.single())
    }

    @Test
    fun the_quantity_never_goes_below_zero_and_an_empty_reason_is_not_sent() = runTest {
        val sut = open()
        sut.onAction(ProductDetailAction.OnQuantityChange(-3))
        assertEquals(0, sut.state.value.shownQuantity)
        sut.onAction(ProductDetailAction.OnQuantitySave)
        assertEquals(Triple("1", 0, null), repo.quantities.single())
    }

    @Test
    fun cancelling_restores_the_stored_quantity() = runTest {
        val sut = open()
        sut.onAction(ProductDetailAction.OnQuantityChange(2))
        sut.reason.setTextAndPlaceCursorAtEnd("x")
        sut.onAction(ProductDetailAction.OnQuantityReset)
        assertNull(sut.state.value.pendingQuantity)
        assertEquals(8, sut.state.value.shownQuantity)
        assertEquals("", sut.reason.text.toString())
    }

    @Test
    fun a_too_long_reason_is_caught_before_sending() = runTest {
        val sut = open()
        sut.onAction(ProductDetailAction.OnQuantityChange(3))
        sut.reason.setTextAndPlaceCursorAtEnd("a".repeat(501))
        sut.onAction(ProductDetailAction.OnQuantitySave)
        assertEquals(setOf(QuantityIssue.REASON_TOO_LONG), sut.state.value.quantityIssues)
        assertTrue(repo.quantities.isEmpty())
    }

    @Test
    fun a_failed_save_keeps_the_pending_quantity_and_says_why() = runTest {
        val sut = open()
        repo.failure = InventoryError.NoInternet
        sut.onAction(ProductDetailAction.OnQuantityChange(3))
        sut.onAction(ProductDetailAction.OnQuantitySave)
        assertEquals(3, sut.state.value.pendingQuantity)
        assertFalse(sut.state.value.isSavingQuantity)
        assertIs<ProductDetailEvent.Message>(events.single())
    }

    @Test
    fun deleting_asks_first_and_then_leaves() = runTest {
        val sut = open()
        sut.onAction(ProductDetailAction.OnDelete)
        assertTrue(sut.state.value.isConfirmingDelete)
        sut.onAction(ProductDetailAction.OnDeleteConfirm)
        assertEquals(ProductDetailEvent.Deleted, events.single())
        assertTrue(repo.products.value.isEmpty())
    }
}
