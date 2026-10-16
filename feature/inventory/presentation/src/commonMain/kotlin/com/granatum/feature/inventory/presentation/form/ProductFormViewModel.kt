package com.granatum.feature.inventory.presentation.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.Dimensions
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.ProductDraft
import com.granatum.feature.inventory.domain.model.ProductModel
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import com.granatum.feature.inventory.domain.validation.DraftIssue
import com.granatum.feature.inventory.domain.validation.InventoryValidation
import com.granatum.feature.inventory.presentation.common.measureLabel
import com.granatum.feature.inventory.presentation.common.toMeasureOrNull
import com.granatum.feature.inventory.presentation.common.toUiText
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.form_saved_without_image
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Numeric fields that did not parse; reported apart from the domain's rules. */
enum class NumberField { HEIGHT, WIDTH, QUANTITY }

/** An image picked on the device, sent to the server when the product is saved. */
class PickedImage(val bytes: ByteArray, val fileName: String, val mimeType: String)

data class ProductFormState(
    val isEdit: Boolean = false,
    val ready: Boolean = false,
    val categories: List<CategoryModel> = emptyList(),
    val categoryId: String? = null,
    val location: StorageLocation? = null,
    /** The product's current image (a signed URL from the server). */
    val imageUrl: String? = null,
    /** A new image, shown at once and uploaded after the product is saved. */
    val pendingImage: PickedImage? = null,
    /** The current image is to be removed on save. */
    val removeImage: Boolean = false,
    val imageError: UiText? = null,
    val issues: Set<DraftIssue> = emptySet(),
    val badNumbers: Set<NumberField> = emptySet(),
    val error: UiText? = null,
    val isSaving: Boolean = false
) {
    val shownImage: Any? get() = pendingImage?.bytes ?: imageUrl?.takeUnless { removeImage }
    val hasImage: Boolean get() = shownImage != null
    val isUploadingImage: Boolean get() = isSaving && pendingImage != null
}

sealed interface ProductFormAction {
    data class OnCategory(val id: String) : ProductFormAction
    data class OnLocation(val location: StorageLocation) : ProductFormAction
    class OnImagePicked(val bytes: ByteArray, val fileName: String, val mimeType: String) : ProductFormAction
    data object OnRemoveImage : ProductFormAction
    data object OnSave : ProductFormAction
}

sealed interface ProductFormEvent {
    data class Saved(val id: String) : ProductFormEvent
}

class ProductFormViewModel(
    productId: String?,
    private val inventory: InventoryUseCases
) : ViewModel() {

    /** Becomes the new product's id once it is created, so a retry edits it instead of duplicating it. */
    private var productId: String? = productId

    val description = TextFieldState()
    val height = TextFieldState()
    val width = TextFieldState()
    val quantity = TextFieldState("0")

    private val ui = MutableStateFlow(ProductFormState(isEdit = productId != null, ready = productId == null))
    private val events = Channel<ProductFormEvent>()
    val eventFlow = events.receiveAsFlow()

    val state = combine(inventory.repository.observeCategories(), ui) { categories, ui -> ui.copy(categories = categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ui.value)

    init {
        if (productId != null) {
            viewModelScope.launch { prefill(inventory.repository.observeProduct(productId).filterNotNull().first()) }
        }
    }

    private fun prefill(p: ProductModel) {
        description.setTextAndPlaceCursorAtEnd(p.description)
        height.setTextAndPlaceCursorAtEnd(p.dimensions.height.measureLabel())
        width.setTextAndPlaceCursorAtEnd(p.dimensions.width.measureLabel())
        quantity.setTextAndPlaceCursorAtEnd(p.quantity.toString())
        ui.update { it.copy(categoryId = p.category.id, location = p.location, imageUrl = p.imageUrl, ready = true) }
    }

    fun onAction(action: ProductFormAction) {
        when (action) {
            is ProductFormAction.OnCategory -> ui.update { it.copy(categoryId = action.id, issues = it.issues - DraftIssue.CATEGORY_REQUIRED) }
            is ProductFormAction.OnLocation -> ui.update { it.copy(location = action.location, issues = it.issues - DraftIssue.LOCATION_REQUIRED) }
            is ProductFormAction.OnImagePicked -> ui.update {
                // The server also refuses anything over the limit; checked here to save the upload.
                if (action.bytes.size > InventoryValidation.IMAGE_MAX_BYTES) {
                    it.copy(imageError = InventoryError.ImageTooLarge.toUiText())
                } else {
                    it.copy(pendingImage = PickedImage(action.bytes, action.fileName, action.mimeType), removeImage = false, imageError = null)
                }
            }
            ProductFormAction.OnRemoveImage -> ui.update {
                it.copy(pendingImage = null, removeImage = it.imageUrl != null, imageError = null)
            }
            ProductFormAction.OnSave -> save()
        }
    }

    private fun save() {
        val current = ui.value
        if (current.isSaving) return
        val bad = mutableSetOf<NumberField>()
        fun measure(field: NumberField, state: TextFieldState): Double =
            state.text.toString().toMeasureOrNull() ?: run { bad += field; 0.0 }
        val count = if (current.isEdit) 0 else quantity.text.toString().trim().toIntOrNull()?.takeIf { it >= 0 } ?: run { bad += NumberField.QUANTITY; 0 }
        val draft = ProductDraft(
            categoryId = current.categoryId,
            description = description.text.toString().trim(),
            dimensions = Dimensions(height = measure(NumberField.HEIGHT, height), width = measure(NumberField.WIDTH, width)),
            quantity = count,
            location = current.location
        )
        val issues = InventoryValidation.validate(draft)
        if (bad.isNotEmpty() || issues.isNotEmpty()) {
            ui.update { it.copy(issues = issues, badNumbers = bad, error = null) }
            return
        }
        ui.update { it.copy(isSaving = true, issues = emptySet(), badNumbers = emptySet(), error = null, imageError = null) }
        viewModelScope.launch {
            val id = productId
            val saved = if (id == null) inventory.repository.create(draft) else inventory.repository.update(id, draft)
            when (saved) {
                is Result.Failure -> ui.update { it.copy(isSaving = false, error = saved.error.toUiText()) }
                is Result.Success -> {
                    productId = saved.data.id
                    ui.update { it.copy(isEdit = true) }
                    saveImage(saved.data.id)
                }
            }
        }
    }

    /**
     * The image goes after the product, because the server stores it against an existing one. If it
     * fails the product is already saved: the form stays open on it so saving again retries only
     * what is missing.
     */
    private suspend fun saveImage(id: String) {
        val current = ui.value
        val result = when {
            current.pendingImage != null -> current.pendingImage.let { inventory.repository.setImage(id, it.bytes, it.fileName, it.mimeType) }
            current.removeImage -> inventory.repository.removeImage(id)
            else -> null
        }
        when (result) {
            null -> events.send(ProductFormEvent.Saved(id))
            is Result.Success -> {
                ui.update { it.copy(isSaving = false, pendingImage = null, removeImage = false, imageUrl = result.data.imageUrl) }
                events.send(ProductFormEvent.Saved(id))
            }
            is Result.Failure -> ui.update {
                it.copy(isSaving = false, imageError = result.error.toUiText(), error = UiText.Resource(Res.string.form_saved_without_image))
            }
        }
    }
}
