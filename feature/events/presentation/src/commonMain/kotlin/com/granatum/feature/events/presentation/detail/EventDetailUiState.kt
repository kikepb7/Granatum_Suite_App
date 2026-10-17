package com.granatum.feature.events.presentation.detail

import androidx.compose.runtime.Immutable
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.presentation.common.EventMaterialUiState

@Immutable
internal sealed interface EventDetailScreenUiState {
    data object Loading : EventDetailScreenUiState
    data object NotFound : EventDetailScreenUiState
    data class Loaded(val data: EventDetailUiState) : EventDetailScreenUiState
}

internal enum class EventDetailDialog { FINISH, DELETE }

@Immutable
internal data class EventDetailUiState(
    val id: String,
    val name: String,
    val description: String?,
    val dayLabel: String,
    val monthLabel: String,
    val dateLabel: String,
    val relativeLabel: UiText,
    val isFinished: Boolean,
    /** `13/06/2027` when it finished; `null` while scheduled. */
    val finishedOnLabel: String?,
    val materials: List<EventMaterialUiState>,
    val materialsLabel: UiText,
    /** The person's role may change events and this one has not finished. */
    val canEdit: Boolean,
    val picker: MaterialPickerUiState?,
    val dialog: EventDetailDialog?,
    /** Finishing or deleting is on its way. */
    val isBusy: Boolean
)

/** The sheet for adding material: what there is, with its free units. */
@Immutable
internal data class MaterialPickerUiState(
    val isLoading: Boolean,
    val error: UiText?,
    val items: List<PickerItemUiState>,
    val selection: PickerSelectionUiState?,
    val isSaving: Boolean
)

@Immutable
internal data class PickerItemUiState(
    val id: String,
    val category: String,
    val description: String,
    val freeUnits: Int,
    /** Units of it the event already takes. */
    val inEvent: Int,
    val locationLabel: UiText,
    val imageUrl: String?
) {
    val isSelectable: Boolean get() = freeUnits > 0
}

/** The material chosen in the sheet and how many more units to add, never more than are free. */
@Immutable
internal data class PickerSelectionUiState(
    val materialId: String,
    val label: String,
    val quantity: Int,
    val max: Int
)

@Immutable
internal sealed interface EventDetailUiAction {
    data object Refresh : EventDetailUiAction
    data class ChangeQuantity(val materialId: String, val quantity: Int) : EventDetailUiAction
    data class RemoveMaterial(val materialId: String) : EventDetailUiAction
    data object OpenPicker : EventDetailUiAction
    data object ClosePicker : EventDetailUiAction
    data object RetryPicker : EventDetailUiAction
    data class SelectPickerMaterial(val materialId: String) : EventDetailUiAction
    data class ChangePickerQuantity(val quantity: Int) : EventDetailUiAction
    data object ConfirmPicker : EventDetailUiAction
    data object RequestFinish : EventDetailUiAction
    data object RequestDelete : EventDetailUiAction
    data object ConfirmDialog : EventDetailUiAction
    data object DismissDialog : EventDetailUiAction
}

internal sealed interface EventDetailUiEvent {
    data class ShowMessage(val message: UiText) : EventDetailUiEvent
    data object Deleted : EventDetailUiEvent
}
