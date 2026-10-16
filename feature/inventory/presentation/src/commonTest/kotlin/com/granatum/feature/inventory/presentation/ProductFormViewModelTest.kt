package com.granatum.feature.inventory.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.granatum.feature.inventory.domain.model.Dimensions
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.model.Warehouse
import com.granatum.feature.inventory.domain.validation.DraftIssue
import com.granatum.feature.inventory.presentation.form.NumberField
import com.granatum.feature.inventory.presentation.form.ProductFormAction
import com.granatum.feature.inventory.presentation.form.ProductFormEvent
import com.granatum.feature.inventory.presentation.form.ProductFormViewModel
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductFormViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val repo = FakeInventoryRepository(listOf(product("1", "Jarrón alto", quantity = 8, location = StorageLocation(Warehouse.B, 4))))
    private val events = mutableListOf<ProductFormEvent>()

    private fun TestScope.open(productId: String? = null): ProductFormViewModel {
        val sut = ProductFormViewModel(productId, useCases(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.eventFlow.collect { events += it } }
        return sut
    }

    private fun ProductFormViewModel.fillNew() {
        onAction(ProductFormAction.OnCategory(VASES.id))
        description.setTextAndPlaceCursorAtEnd("  Cilindro de cristal ")
        height.setTextAndPlaceCursorAtEnd("40,5")
        width.setTextAndPlaceCursorAtEnd("12")
        quantity.setTextAndPlaceCursorAtEnd("6")
        onAction(ProductFormAction.OnLocation(StorageLocation(Warehouse.C, 3)))
    }

    @Test
    fun a_new_product_is_saved_and_then_its_image_uploaded() = runTest {
        val sut = open()
        sut.fillNew()
        sut.onAction(ProductFormAction.OnImagePicked(byteArrayOf(1, 2), "foto.jpg", "image/jpeg"))
        sut.onAction(ProductFormAction.OnSave)

        val draft = repo.created.single()
        assertEquals("Cilindro de cristal", draft.description)
        assertEquals(Dimensions(40.5, 12.0), draft.dimensions)
        assertEquals(6, draft.quantity)
        assertEquals(StorageLocation(Warehouse.C, 3), draft.location)
        assertEquals("new" to "foto.jpg", repo.images.single())
        assertEquals(ProductFormEvent.Saved("new"), events.single())
    }

    @Test
    fun without_an_image_nothing_else_is_sent() = runTest {
        val sut = open()
        sut.fillNew()
        sut.onAction(ProductFormAction.OnSave)
        assertTrue(repo.images.isEmpty() && repo.removedImages.isEmpty())
        assertEquals(ProductFormEvent.Saved("new"), events.single())
    }

    @Test
    fun missing_fields_and_bad_numbers_are_reported_without_sending() = runTest {
        val sut = open()
        sut.height.setTextAndPlaceCursorAtEnd("alto")
        sut.width.setTextAndPlaceCursorAtEnd("0")
        sut.quantity.setTextAndPlaceCursorAtEnd("-1")
        sut.onAction(ProductFormAction.OnSave)
        val state = sut.state.value
        assertEquals(
            setOf(DraftIssue.CATEGORY_REQUIRED, DraftIssue.DESCRIPTION_REQUIRED, DraftIssue.LOCATION_REQUIRED, DraftIssue.INVALID_SIZE),
            state.issues
        )
        assertEquals(setOf(NumberField.HEIGHT, NumberField.QUANTITY), state.badNumbers)
        assertTrue(repo.created.isEmpty())

        sut.onAction(ProductFormAction.OnLocation(StorageLocation(Warehouse.A, 1)))
        assertFalse(DraftIssue.LOCATION_REQUIRED in sut.state.value.issues)
    }

    @Test
    fun editing_prefills_and_never_sends_a_quantity_change() = runTest {
        val sut = open("1")
        assertTrue(sut.state.value.ready)
        assertEquals("Jarrón alto", sut.description.text.toString())
        assertEquals(StorageLocation(Warehouse.B, 4), sut.state.value.location)
        sut.quantity.setTextAndPlaceCursorAtEnd("99")
        sut.onAction(ProductFormAction.OnSave)
        val (id, draft) = repo.updated.single()
        assertEquals("1", id)
        assertEquals(0, draft.quantity)
    }

    @Test
    fun removing_the_current_image_is_sent_on_save() = runTest {
        val sut = open("1")
        sut.onAction(ProductFormAction.OnRemoveImage)
        assertFalse(sut.state.value.hasImage)
        sut.onAction(ProductFormAction.OnSave)
        assertEquals(listOf("1"), repo.removedImages)
        assertEquals(ProductFormEvent.Saved("1"), events.single())
    }

    @Test
    fun the_upload_shows_while_saving() = runTest {
        val gate = CompletableDeferred<Unit>().also { repo.imageGate = it }
        val sut = open("1")
        sut.onAction(ProductFormAction.OnImagePicked(byteArrayOf(9), "a.png", "image/png"))
        assertNotNull(sut.state.value.shownImage)
        sut.onAction(ProductFormAction.OnSave)
        assertTrue(sut.state.value.isUploadingImage)
        gate.complete(Unit)
        assertFalse(sut.state.value.isSaving)
        assertEquals(ProductFormEvent.Saved("1"), events.single())
    }

    @Test
    fun a_failed_upload_keeps_the_saved_product_and_retries_only_by_editing_it() = runTest {
        repo.imageFailure = InventoryError.ImageInvalid
        val sut = open()
        sut.fillNew()
        sut.onAction(ProductFormAction.OnImagePicked(byteArrayOf(9), "a.heic", "image/heic"))
        sut.onAction(ProductFormAction.OnSave)

        assertTrue(events.isEmpty())
        assertNotNull(sut.state.value.imageError)
        assertNotNull(sut.state.value.error)
        assertTrue(sut.state.value.isEdit)
        assertNotNull(sut.state.value.pendingImage)

        repo.imageFailure = null
        sut.onAction(ProductFormAction.OnSave)
        assertEquals(1, repo.created.size, "not created twice")
        assertEquals("new", repo.updated.single().first)
        assertEquals(ProductFormEvent.Saved("new"), events.single())
    }

    @Test
    fun an_oversized_image_is_refused_on_the_device() = runTest {
        val sut = open("1")
        sut.onAction(ProductFormAction.OnImagePicked(ByteArray(10 * 1024 * 1024 + 1), "big.jpg", "image/jpeg"))
        assertNull(sut.state.value.pendingImage)
        assertNotNull(sut.state.value.imageError)
    }
}
