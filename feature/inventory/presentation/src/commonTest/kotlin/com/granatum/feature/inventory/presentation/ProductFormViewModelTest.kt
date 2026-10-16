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
    private val images = FakeImageStorage()
    private val events = mutableListOf<ProductFormEvent>()

    private fun TestScope.open(productId: String? = null): ProductFormViewModel {
        val sut = ProductFormViewModel(productId, useCases(repo, images))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sut.eventFlow.collect { events += it } }
        return sut
    }

    @Test
    fun a_new_product_is_saved_with_every_field() = runTest {
        val sut = open()
        sut.onAction(ProductFormAction.OnCategory(VASES.id))
        sut.description.setTextAndPlaceCursorAtEnd("  Cilindro de cristal ")
        sut.height.setTextAndPlaceCursorAtEnd("40,5")
        sut.width.setTextAndPlaceCursorAtEnd("12")
        sut.quantity.setTextAndPlaceCursorAtEnd("6")
        sut.onAction(ProductFormAction.OnLocation(StorageLocation(Warehouse.C, 3)))
        sut.onAction(ProductFormAction.OnImagePicked(byteArrayOf(1, 2), "image/jpeg"))
        sut.onAction(ProductFormAction.OnSave)

        val draft = repo.created.single()
        assertEquals("Cilindro de cristal", draft.description)
        assertEquals(Dimensions(40.5, 12.0), draft.dimensions)
        assertEquals(6, draft.quantity)
        assertEquals(StorageLocation(Warehouse.C, 3), draft.location)
        assertEquals(images.url, draft.imageUrl)
        assertEquals(ProductFormEvent.Saved("new"), events.single())
    }

    @Test
    fun missing_fields_and_bad_numbers_are_reported_without_sending() = runTest {
        val sut = open()
        sut.height.setTextAndPlaceCursorAtEnd("alto")
        sut.quantity.setTextAndPlaceCursorAtEnd("-1")
        sut.onAction(ProductFormAction.OnSave)
        val state = sut.state.value
        assertEquals(setOf(DraftIssue.CATEGORY_REQUIRED, DraftIssue.DESCRIPTION_REQUIRED, DraftIssue.LOCATION_REQUIRED), state.issues)
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
        assertEquals("https://example.test/1.jpg", draft.imageUrl)
    }

    @Test
    fun saving_waits_for_the_image_upload() = runTest {
        val gate = CompletableDeferred<Unit>().also { images.gate = it }
        val sut = open("1")
        sut.onAction(ProductFormAction.OnImagePicked(byteArrayOf(9), "image/png"))
        assertTrue(sut.state.value.isUploadingImage)
        assertNotNull(sut.state.value.imagePreview)
        sut.onAction(ProductFormAction.OnSave)
        assertTrue(repo.updated.isEmpty())

        gate.complete(Unit)
        assertFalse(sut.state.value.isUploadingImage)
        assertEquals(images.url, sut.state.value.imageUrl)
        sut.onAction(ProductFormAction.OnSave)
        assertEquals(images.url, repo.updated.single().second.imageUrl)
    }

    @Test
    fun a_failed_upload_drops_the_preview_and_keeps_the_previous_image() = runTest {
        images.failure = InventoryError.ImageUploadFailed
        val sut = open("1")
        sut.onAction(ProductFormAction.OnImagePicked(byteArrayOf(9), "image/png"))
        assertNull(sut.state.value.imagePreview)
        assertNotNull(sut.state.value.imageError)
        assertEquals("https://example.test/1.jpg", sut.state.value.imageUrl)
    }

    @Test
    fun an_oversized_image_is_refused_on_the_device_and_removing_clears_it() = runTest {
        val sut = open("1")
        sut.onAction(ProductFormAction.OnImagePicked(ByteArray(10 * 1024 * 1024 + 1), "image/jpeg"))
        assertTrue(images.uploads.isEmpty())
        assertNotNull(sut.state.value.imageError)
        sut.onAction(ProductFormAction.OnRemoveImage)
        assertNull(sut.state.value.imageUrl)
        assertFalse(sut.state.value.hasImage)
    }

    @Test
    fun an_environment_without_storage_says_so() = runTest {
        val sut = ProductFormViewModel(null, useCases(repo, FakeImageStorage(isAvailable = false)))
        assertFalse(sut.state.value.canUploadImages)
    }
}
