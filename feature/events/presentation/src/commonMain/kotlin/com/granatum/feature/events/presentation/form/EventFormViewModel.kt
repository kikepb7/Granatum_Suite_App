package com.granatum.feature.events.presentation.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.domain.model.EventDraft
import com.granatum.feature.events.domain.provider.TodayProvider
import com.granatum.feature.events.domain.usecase.ObserveEventUseCase
import com.granatum.feature.events.domain.usecase.ObserveEventsUseCase
import com.granatum.feature.events.domain.usecase.SaveEventUseCase
import com.granatum.feature.events.domain.validation.EventIssue
import com.granatum.feature.events.domain.validation.EventValidation
import com.granatum.feature.events.presentation.common.EventDateFormatter
import com.granatum.feature.events.presentation.common.toUiText
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.date_in_past
import granatumsuite.feature.events.presentation.generated.resources.date_required
import granatumsuite.feature.events.presentation.generated.resources.name_required
import granatumsuite.feature.events.presentation.generated.resources.too_long
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** The raw, typed values behind the form; [EventFormUiState] is derived from it. */
private data class EventFormInternalState(
    val isReady: Boolean,
    val date: LocalDate? = null,
    val issues: Set<EventIssue> = emptySet(),
    val dateMissing: Boolean = false,
    val isSaving: Boolean = false
)

internal class EventFormViewModel(
    private val eventId: String?,
    observeEventUseCase: ObserveEventUseCase,
    observeEventsUseCase: ObserveEventsUseCase,
    private val saveEventUseCase: SaveEventUseCase,
    private val todayProvider: TodayProvider,
    private val dates: EventDateFormatter
) : ViewModel() {

    val name = TextFieldState()
    val description = TextFieldState()

    private val internalState = MutableStateFlow(EventFormInternalState(isReady = false))
    private val _uiEvents = MutableSharedFlow<EventFormUiEvent>()
    val uiEvents: SharedFlow<EventFormUiEvent> = _uiEvents.asSharedFlow()

    val uiState: StateFlow<EventFormScreenUiState> = internalState
        .map { it.toScreenUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventFormScreenUiState.Loading)

    init {
        viewModelScope.launch {
            if (eventId == null) {
                // «Evento N»: what the person would most likely type, ready to change.
                name.setTextAndPlaceCursorAtEnd(EventValidation.suggestedName(observeEventsUseCase.observe().first().size))
                internalState.update { it.copy(isReady = true) }
            } else {
                val event = observeEventUseCase.observe(eventId).filterNotNull().first()
                name.setTextAndPlaceCursorAtEnd(event.name)
                description.setTextAndPlaceCursorAtEnd(event.description.orEmpty())
                internalState.update { it.copy(isReady = true, date = event.date) }
            }
        }
    }

    fun onAction(action: EventFormUiAction) {
        when (action) {
            is EventFormUiAction.DateSelected -> internalState.update {
                it.copy(date = action.date, dateMissing = false, issues = it.issues - EventIssue.DATE_IN_PAST)
            }
            EventFormUiAction.Save -> save()
        }
    }

    private fun save() {
        val current = internalState.value
        if (current.isSaving || !current.isReady) return
        val date = current.date
        if (date == null) {
            internalState.update { it.copy(dateMissing = true, issues = validateTexts()) }
            return
        }
        val draft = EventDraft(name = name.text.toString(), description = description.text.toString(), date = date)
        val issues = EventValidation.validate(draft, todayProvider.today())
        if (issues.isNotEmpty()) {
            internalState.update { it.copy(issues = issues, dateMissing = false) }
            return
        }
        internalState.update { it.copy(isSaving = true, issues = emptySet(), dateMissing = false) }
        viewModelScope.launch {
            when (val result = saveEventUseCase.save(eventId, draft)) {
                is Result.Success -> _uiEvents.emit(EventFormUiEvent.Saved(result.data.id))
                is Result.Failure -> _uiEvents.emit(EventFormUiEvent.ShowError(result.error.toUiText()))
            }
            internalState.update { it.copy(isSaving = false) }
        }
    }

    /** The text rules alone, for when the date is still missing. */
    private fun validateTexts(): Set<EventIssue> =
        EventValidation.validate(EventDraft(name.text.toString(), description.text.toString(), todayProvider.today()), todayProvider.today())

    private fun EventFormInternalState.toScreenUiState(): EventFormScreenUiState {
        if (!isReady) return EventFormScreenUiState.Loading
        return EventFormScreenUiState.Loaded(
            EventFormUiState(
                isEdit = eventId != null,
                date = date,
                dateLabel = date?.let(dates::long),
                minDate = todayProvider.today(),
                nameError = when {
                    EventIssue.NAME_REQUIRED in issues -> UiText.Resource(Res.string.name_required)
                    EventIssue.NAME_TOO_LONG in issues -> UiText.Resource(Res.string.too_long, arrayOf(EventValidation.NAME_MAX))
                    else -> null
                },
                descriptionError = if (EventIssue.DESCRIPTION_TOO_LONG in issues) {
                    UiText.Resource(Res.string.too_long, arrayOf(EventValidation.DESCRIPTION_MAX))
                } else {
                    null
                },
                dateError = when {
                    dateMissing -> UiText.Resource(Res.string.date_required)
                    EventIssue.DATE_IN_PAST in issues -> UiText.Resource(Res.string.date_in_past)
                    else -> null
                },
                isSaving = isSaving
            )
        )
    }
}
