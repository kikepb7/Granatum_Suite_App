package com.granatum.feature.events.presentation.form

import androidx.compose.runtime.Immutable
import com.granatum.core.presentation.util.UiText
import kotlinx.datetime.LocalDate

@Immutable
internal sealed interface EventFormScreenUiState {
    data object Loading : EventFormScreenUiState
    data class Loaded(val data: EventFormUiState) : EventFormScreenUiState
}

@Immutable
internal data class EventFormUiState(
    val isEdit: Boolean,
    val date: LocalDate?,
    /** `sáb, 13 jun 2027`, or `null` while no date is chosen. */
    val dateLabel: String?,
    /** The earliest day the calendar offers: today. */
    val minDate: LocalDate,
    val nameError: UiText?,
    val descriptionError: UiText?,
    val dateError: UiText?,
    val isSaving: Boolean
)

@Immutable
internal sealed interface EventFormUiAction {
    data class DateSelected(val date: LocalDate) : EventFormUiAction
    data object Save : EventFormUiAction
}

internal sealed interface EventFormUiEvent {
    data class Saved(val eventId: String) : EventFormUiEvent
    data class ShowError(val message: UiText) : EventFormUiEvent
}
