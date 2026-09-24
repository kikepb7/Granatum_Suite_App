package com.granatum.feature.inventory.presentation.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result.Failure
import com.granatum.core.domain.util.Result.Success
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.MaterialCategory
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.usecase.GetMaterialDetailUseCase
import com.granatum.feature.inventory.domain.usecase.SaveMaterialUseCase
import com.granatum.feature.inventory.presentation.mapper.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [materialId] is null when creating a new material. */
class MaterialFormViewModel(
    private val materialId: String?,
    private val getMaterialDetailUseCase: GetMaterialDetailUseCase,
    private val saveMaterialUseCase: SaveMaterialUseCase
) : ViewModel() {

    val nameState = TextFieldState()
    val sizeState = TextFieldState()
    val colorState = TextFieldState()
    val locationState = TextFieldState()
    val notesState = TextFieldState()
    val quantityState = TextFieldState(initialText = "0")

    private val eventChannel = Channel<MaterialFormEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state = MutableStateFlow(MaterialFormUiState(isEditing = materialId != null))
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MaterialFormUiState(isEditing = materialId != null)
    )

    init {
        if (materialId != null) {
            viewModelScope.launch {
                val existing = getMaterialDetailUseCase(materialId = materialId).first()
                existing?.let(::prefill)
            }
        }
    }

    fun onAction(action: MaterialFormAction) {
        when (action) {
            is MaterialFormAction.OnCategorySelected -> _state.update { it.copy(category = action.category) }
            MaterialFormAction.OnSaveClick -> save()
        }
    }

    private fun prefill(material: MaterialModel) {
        nameState.edit { replace(0, length, material.name) }
        sizeState.edit { replace(0, length, material.size.orEmpty()) }
        colorState.edit { replace(0, length, material.color.orEmpty()) }
        locationState.edit { replace(0, length, material.location.orEmpty()) }
        notesState.edit { replace(0, length, material.notes.orEmpty()) }
        quantityState.edit { replace(0, length, material.quantity.toString()) }
        _state.update { it.copy(category = material.category, photoUrls = material.photoUrls) }
    }

    private fun save() {
        val draft = MaterialDraft(
            id = materialId,
            name = nameState.text.toString().trim(),
            category = state.value.category,
            quantity = quantityState.text.toString().toIntOrNull() ?: -1,
            size = sizeState.text.toString().trim().ifBlank { null },
            color = colorState.text.toString().trim().ifBlank { null },
            location = locationState.text.toString().trim().ifBlank { null },
            photoUrls = state.value.photoUrls,
            notes = notesState.text.toString().trim().ifBlank { null }
        )

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val result = saveMaterialUseCase(draft = draft)) {
                is Success -> eventChannel.send(MaterialFormEvent.Saved(result.data.id))
                is Failure -> eventChannel.send(MaterialFormEvent.Error(result.error.toUiText()))
            }
            _state.update { it.copy(isSaving = false) }
        }
    }
}

data class MaterialFormUiState(
    val isEditing: Boolean,
    val category: MaterialCategory = MaterialCategory.OTROS,
    val photoUrls: List<String> = emptyList(),
    val isSaving: Boolean = false
)

sealed interface MaterialFormAction {
    data class OnCategorySelected(val category: MaterialCategory) : MaterialFormAction
    data object OnSaveClick : MaterialFormAction
}

sealed interface MaterialFormEvent {
    data class Saved(val materialId: String) : MaterialFormEvent
    data class Error(val message: UiText) : MaterialFormEvent
}
