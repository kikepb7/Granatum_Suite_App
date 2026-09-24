package com.granatum.feature.inventory.presentation.detail

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result.Failure
import com.granatum.core.domain.util.Result.Success
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.StockMovementModel
import com.granatum.feature.inventory.domain.usecase.GetMaterialDetailUseCase
import com.granatum.feature.inventory.domain.usecase.GetStockHistoryUseCase
import com.granatum.feature.inventory.domain.usecase.UpdateMaterialQuantityUseCase
import com.granatum.feature.inventory.presentation.mapper.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MaterialDetailViewModel(
    private val materialId: String,
    private val getMaterialDetailUseCase: GetMaterialDetailUseCase,
    private val getStockHistoryUseCase: GetStockHistoryUseCase,
    private val updateMaterialQuantityUseCase: UpdateMaterialQuantityUseCase
) : ViewModel() {

    val newQuantityState = TextFieldState()
    val reasonState = TextFieldState()

    private val _uiState = MutableStateFlow(MaterialDetailUiState())

    private val eventChannel = Channel<MaterialDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    val state = combine(
        _uiState,
        getMaterialDetailUseCase(materialId = materialId),
        getStockHistoryUseCase(materialId = materialId)
    ) { uiState, material, history ->
        uiState.copy(material = material, history = history)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = MaterialDetailUiState()
        )

    fun onAction(action: MaterialDetailAction) {
        when (action) {
            MaterialDetailAction.OnEditClick -> sendEvent(MaterialDetailEvent.NavigateToEdit(materialId))
            MaterialDetailAction.OnUpdateQuantityClick -> openQuantitySheet()
            MaterialDetailAction.OnDismissQuantitySheet -> dismissQuantitySheet()
            MaterialDetailAction.OnConfirmQuantityUpdate -> confirmQuantityUpdate()
        }
    }

    private fun openQuantitySheet() {
        val current = state.value.material?.quantity ?: return
        newQuantityState.edit { replace(0, length, current.toString()) }
        reasonState.clearText()
        _uiState.update { it.copy(isQuantitySheetVisible = true) }
    }

    private fun dismissQuantitySheet() {
        _uiState.update { it.copy(isQuantitySheetVisible = false) }
    }

    private fun confirmQuantityUpdate() {
        val material = state.value.material ?: return
        val newQuantity = newQuantityState.text.toString().toIntOrNull()
        if (newQuantity == null) {
            sendEvent(MaterialDetailEvent.Error(UiText.DynamicString(value = "Introduce una cantidad válida")))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingQuantity = true) }
            when (
                val result = updateMaterialQuantityUseCase(
                    materialId = material.id,
                    currentQuantity = material.quantity,
                    newQuantity = newQuantity,
                    reason = reasonState.text.toString()
                )
            ) {
                is Success -> _uiState.update { it.copy(isQuantitySheetVisible = false) }
                is Failure -> eventChannel.send(MaterialDetailEvent.Error(result.error.toUiText()))
            }
            _uiState.update { it.copy(isUpdatingQuantity = false) }
        }
    }

    private fun sendEvent(event: MaterialDetailEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}

data class MaterialDetailUiState(
    val material: MaterialModel? = null,
    val history: List<StockMovementModel> = emptyList(),
    val isQuantitySheetVisible: Boolean = false,
    val isUpdatingQuantity: Boolean = false
)

sealed interface MaterialDetailAction {
    data object OnEditClick : MaterialDetailAction
    data object OnUpdateQuantityClick : MaterialDetailAction
    data object OnDismissQuantitySheet : MaterialDetailAction
    data object OnConfirmQuantityUpdate : MaterialDetailAction
}

sealed interface MaterialDetailEvent {
    data class NavigateToEdit(val materialId: String) : MaterialDetailEvent
    data class Error(val message: UiText) : MaterialDetailEvent
}
