package com.granatum.feature.inventory.presentation.detail

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.MaterialHistoryEntry
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import com.granatum.feature.inventory.domain.validation.AdjustmentIssue
import com.granatum.feature.inventory.domain.validation.InventoryValidation
import com.granatum.feature.inventory.presentation.common.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MaterialDetailState(
    val material: MaterialModel? = null,
    val loaded: Boolean = false,
    val history: List<MaterialHistoryEntry> = emptyList(),
    val isAdjusting: Boolean = false,
    val adjustIssues: Set<AdjustmentIssue> = emptySet(),
    val adjustNotANumber: Boolean = false,
    val isConfirmingDelete: Boolean = false,
    val isBusy: Boolean = false
)

sealed interface MaterialDetailAction {
    data object OnAdjust : MaterialDetailAction
    data object OnAdjustDismiss : MaterialDetailAction
    data object OnAdjustSave : MaterialDetailAction
    data object OnDelete : MaterialDetailAction
    data object OnDeleteDismiss : MaterialDetailAction
    data object OnDeleteConfirm : MaterialDetailAction
}

sealed interface MaterialDetailEvent {
    data class Message(val text: UiText) : MaterialDetailEvent
    data object Deleted : MaterialDetailEvent
}

class MaterialDetailViewModel(
    private val materialId: String,
    private val inventory: InventoryUseCases
) : ViewModel() {

    val newQuantity = TextFieldState()
    val reason = TextFieldState()
    private val ui = MutableStateFlow(MaterialDetailState())
    private val events = Channel<MaterialDetailEvent>()
    val eventFlow = events.receiveAsFlow()

    val state = combine(
        inventory.repository.observeMaterial(materialId),
        inventory.repository.observeHistory(materialId),
        ui
    ) { material, history, ui -> ui.copy(material = material, history = history, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), MaterialDetailState())

    init {
        // Fresh data when coverage allows; the cached copy otherwise. A material deleted
        // elsewhere disappears from the cache here (FR-015).
        viewModelScope.launch {
            inventory.repository.refreshMaterial(materialId)
            inventory.repository.refreshHistory(materialId)
        }
    }

    fun onAction(action: MaterialDetailAction) {
        when (action) {
            MaterialDetailAction.OnAdjust -> {
                newQuantity.setTextAndPlaceCursorAtEnd(state.value.material?.available?.toString().orEmpty())
                reason.setTextAndPlaceCursorAtEnd("")
                ui.update { it.copy(isAdjusting = true, adjustIssues = emptySet(), adjustNotANumber = false) }
            }
            MaterialDetailAction.OnAdjustDismiss -> ui.update { it.copy(isAdjusting = false) }
            MaterialDetailAction.OnAdjustSave -> saveAdjustment()
            MaterialDetailAction.OnDelete -> ui.update { it.copy(isConfirmingDelete = true) }
            MaterialDetailAction.OnDeleteDismiss -> ui.update { it.copy(isConfirmingDelete = false) }
            MaterialDetailAction.OnDeleteConfirm -> delete()
        }
    }

    private fun saveAdjustment() {
        val material = state.value.material ?: return
        if (ui.value.isBusy) return
        val value = newQuantity.text.toString().trim().toIntOrNull()
        if (value == null) {
            ui.update { it.copy(adjustNotANumber = true, adjustIssues = emptySet()) }
            return
        }
        val issues = InventoryValidation.validateAdjustment(material.available, material.total, value, reason.text.toString())
        if (issues.isNotEmpty()) {
            ui.update { it.copy(adjustIssues = issues, adjustNotANumber = false) }
            return
        }
        ui.update { it.copy(isBusy = true, adjustIssues = emptySet(), adjustNotANumber = false) }
        viewModelScope.launch {
            when (val result = inventory.repository.adjustQuantity(materialId, value, reason.text.toString())) {
                is Result.Success -> {
                    ui.update { it.copy(isBusy = false, isAdjusting = false) }
                    inventory.repository.refreshHistory(materialId)
                }
                is Result.Failure -> {
                    ui.update { it.copy(isBusy = false) }
                    events.send(MaterialDetailEvent.Message(result.error.toUiText()))
                }
            }
        }
    }

    private fun delete() {
        if (ui.value.isBusy) return
        ui.update { it.copy(isBusy = true, isConfirmingDelete = false) }
        viewModelScope.launch {
            when (val result = inventory.repository.delete(materialId)) {
                is Result.Success -> events.send(MaterialDetailEvent.Deleted)
                is Result.Failure -> events.send(MaterialDetailEvent.Message(result.error.toUiText()))
            }
            ui.update { it.copy(isBusy = false) }
        }
    }
}
