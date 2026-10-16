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
import kotlinx.coroutines.Job
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

data class ProductFormState(
    val isEdit: Boolean = false,
    val ready: Boolean = false,
    val categories: List<CategoryModel> = emptyList(),
    val categoryId: String? = null,
    val location: StorageLocation? = null,
    /** The stored image: the product's own, or the one just uploaded. */
    val imageUrl: String? = null,
    /** The picked bytes, shown at once while they upload. */
    val imagePreview: ByteArray? = null,
    val isUploadingImage: Boolean = false,
    val imageError: UiText? = null,
    val canUploadImages: Boolean = true,
    val issues: Set<DraftIssue> = emptySet(),
    val badNumbers: Set<NumberField> = emptySet(),
    val error: UiText? = null,
    val isSaving: Boolean = false
) {
    val shownImage: Any? get() = imagePreview ?: imageUrl
    val hasImage: Boolean get() = shownImage != null
}

sealed interface ProductFormAction {
    data class OnCategory(val id: String) : ProductFormAction
    data class OnLocation(val location: StorageLocation) : ProductFormAction
    class OnImagePicked(val bytes: ByteArray, val mimeType: String) : ProductFormAction
    data object OnRemoveImage : ProductFormAction
    data object OnSave : ProductFormAction
}

sealed interface ProductFormEvent {
    data class Saved(val id: String) : ProductFormEvent
}

class ProductFormViewModel(
    private val productId: String?,
    private val inventory: InventoryUseCases
) : ViewModel() {

    val description = TextFieldState()
    val height = TextFieldState()
    val width = TextFieldState()
    val quantity = TextFieldState("0")

    private val ui = MutableStateFlow(
        ProductFormState(isEdit = productId != null, ready = productId == null, canUploadImages = inventory.images.isAvailable)
    )
    private val events = Channel<ProductFormEvent>()
    val eventFlow = events.receiveAsFlow()
    private var upload: Job? = null

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
            is ProductFormAction.OnImagePicked -> uploadImage(action.bytes, action.mimeType)
            ProductFormAction.OnRemoveImage -> {
                upload?.cancel()
                ui.update { it.copy(imageUrl = null, imagePreview = null, isUploadingImage = false, imageError = null) }
            }
            ProductFormAction.OnSave -> save()
        }
    }

    /**
     * Uploads as soon as the image is picked, so saving only sends its URL. The previous image, if
     * any, stays in the storage: nothing on the server references it any more, and removing files
     * needs a privilege the app does not have (research D3).
     */
    private fun uploadImage(bytes: ByteArray, mimeType: String) {
        if (bytes.size > InventoryValidation.IMAGE_MAX_BYTES) {
            ui.update { it.copy(imageError = InventoryError.ImageTooLarge.toUiText()) }
            return
        }
        upload?.cancel()
        ui.update { it.copy(imagePreview = bytes, isUploadingImage = true, imageError = null) }
        upload = viewModelScope.launch {
            when (val result = inventory.images.upload(bytes, mimeType)) {
                is Result.Success -> ui.update { it.copy(imageUrl = result.data, isUploadingImage = false) }
                is Result.Failure -> ui.update {
                    it.copy(imagePreview = null, isUploadingImage = false, imageError = result.error.toUiText())
                }
            }
        }
    }

    private fun save() {
        val current = ui.value
        if (current.isSaving || current.isUploadingImage) return
        val bad = mutableSetOf<NumberField>()
        fun measure(field: NumberField, state: TextFieldState): Double =
            state.text.toString().let { if (it.isBlank()) 0.0 else it.toMeasureOrNull() } ?: run { bad += field; 0.0 }
        val count = if (current.isEdit) 0 else quantity.text.toString().trim().toIntOrNull()?.takeIf { it >= 0 } ?: run { bad += NumberField.QUANTITY; 0 }
        val draft = ProductDraft(
            categoryId = current.categoryId,
            description = description.text.toString().trim(),
            dimensions = Dimensions(height = measure(NumberField.HEIGHT, height), width = measure(NumberField.WIDTH, width)),
            quantity = count,
            imageUrl = current.imageUrl,
            location = current.location
        )
        val issues = InventoryValidation.validate(draft)
        if (bad.isNotEmpty() || issues.isNotEmpty()) {
            ui.update { it.copy(issues = issues, badNumbers = bad, error = null) }
            return
        }
        ui.update { it.copy(isSaving = true, issues = emptySet(), badNumbers = emptySet(), error = null) }
        viewModelScope.launch {
            val result = if (productId == null) inventory.repository.create(draft) else inventory.repository.update(productId, draft)
            when (result) {
                is Result.Success -> events.send(ProductFormEvent.Saved(result.data.id))
                is Result.Failure -> ui.update { it.copy(error = result.error.toUiText()) }
            }
            ui.update { it.copy(isSaving = false) }
        }
    }
}
