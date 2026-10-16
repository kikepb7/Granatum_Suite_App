package com.granatum.feature.inventory.presentation.detail

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.ProductHistoryEntry
import com.granatum.feature.inventory.domain.model.ProductModel
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import com.granatum.feature.inventory.domain.validation.InventoryValidation
import com.granatum.feature.inventory.domain.validation.QuantityIssue
import com.granatum.feature.inventory.presentation.common.toUiText
import granatumsuite.feature.inventory.presentation.generated.resources.Res
import granatumsuite.feature.inventory.presentation.generated.resources.quantity_saved
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailState(
    val product: ProductModel? = null,
    val loaded: Boolean = false,
    val history: List<ProductHistoryEntry> = emptyList(),
    /** The quantity being prepared with the stepper; null while it matches the stored one. */
    val pendingQuantity: Int? = null,
    val quantityIssues: Set<QuantityIssue> = emptySet(),
    val isSavingQuantity: Boolean = false,
    val isConfirmingDelete: Boolean = false,
    val isDeleting: Boolean = false
) {
    val shownQuantity: Int get() = pendingQuantity ?: product?.quantity ?: 0
    val hasPendingQuantity: Boolean get() = pendingQuantity != null && pendingQuantity != product?.quantity
}

sealed interface ProductDetailAction {
    data class OnQuantityChange(val value: Int) : ProductDetailAction
    data object OnQuantityReset : ProductDetailAction
    data object OnQuantitySave : ProductDetailAction
    data object OnDelete : ProductDetailAction
    data object OnDeleteDismiss : ProductDetailAction
    data object OnDeleteConfirm : ProductDetailAction
}

sealed interface ProductDetailEvent {
    data class Message(val text: UiText) : ProductDetailEvent
    data object Deleted : ProductDetailEvent
}

class ProductDetailViewModel(
    private val productId: String,
    private val inventory: InventoryUseCases
) : ViewModel() {

    val reason = TextFieldState()
    private val ui = MutableStateFlow(ProductDetailState())
    private val events = Channel<ProductDetailEvent>()
    val eventFlow = events.receiveAsFlow()

    val state = combine(
        inventory.repository.observeProduct(productId),
        inventory.repository.observeHistory(productId),
        ui
    ) { product, history, ui -> ui.copy(product = product, history = history, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ProductDetailState())

    init {
        // Fresh data when coverage allows; the cached copy otherwise. A product deleted elsewhere
        // disappears from the cache here.
        viewModelScope.launch {
            inventory.repository.refreshProduct(productId)
            inventory.repository.refreshHistory(productId)
        }
    }

    fun onAction(action: ProductDetailAction) {
        when (action) {
            is ProductDetailAction.OnQuantityChange -> ui.update {
                it.copy(pendingQuantity = action.value.coerceAtLeast(0), quantityIssues = emptySet())
            }
            ProductDetailAction.OnQuantityReset -> {
                reason.clearText()
                ui.update { it.copy(pendingQuantity = null, quantityIssues = emptySet()) }
            }
            ProductDetailAction.OnQuantitySave -> saveQuantity()
            ProductDetailAction.OnDelete -> ui.update { it.copy(isConfirmingDelete = true) }
            ProductDetailAction.OnDeleteDismiss -> ui.update { it.copy(isConfirmingDelete = false) }
            ProductDetailAction.OnDeleteConfirm -> delete()
        }
    }

    private fun saveQuantity() {
        val current = state.value
        val product = current.product ?: return
        val target = current.pendingQuantity ?: return
        if (current.isSavingQuantity) return
        val reasonText = reason.text.toString().trim().ifEmpty { null }
        val issues = InventoryValidation.validateQuantity(product.quantity, target, reasonText)
        if (issues.isNotEmpty()) {
            ui.update { it.copy(quantityIssues = issues) }
            return
        }
        ui.update { it.copy(isSavingQuantity = true) }
        viewModelScope.launch {
            when (val result = inventory.repository.setQuantity(productId, target, reasonText)) {
                is Result.Success -> {
                    reason.clearText()
                    ui.update { it.copy(isSavingQuantity = false, pendingQuantity = null) }
                    events.send(ProductDetailEvent.Message(UiText.Resource(Res.string.quantity_saved)))
                    inventory.repository.refreshHistory(productId)
                }
                is Result.Failure -> {
                    ui.update { it.copy(isSavingQuantity = false) }
                    events.send(ProductDetailEvent.Message(result.error.toUiText()))
                }
            }
        }
    }

    private fun delete() {
        if (ui.value.isDeleting) return
        ui.update { it.copy(isDeleting = true, isConfirmingDelete = false) }
        viewModelScope.launch {
            when (val result = inventory.repository.delete(productId)) {
                is Result.Success -> events.send(ProductDetailEvent.Deleted)
                is Result.Failure -> events.send(ProductDetailEvent.Message(result.error.toUiText()))
            }
            ui.update { it.copy(isDeleting = false) }
        }
    }
}
