package com.granatum.feature.events.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.provider.TodayProvider
import com.granatum.feature.events.domain.usecase.ObserveEventsUseCase
import com.granatum.feature.events.domain.usecase.RefreshEventsUseCase
import com.granatum.feature.events.presentation.common.EventUiMapper
import com.granatum.feature.events.presentation.common.toUiText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the ViewModel keeps that the events do not say: the tab and how the last refresh went. */
private data class EventListInternalState(
    val selectedTab: EventTab = EventTab.UPCOMING,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false
)

internal class EventListViewModel(
    observeEventsUseCase: ObserveEventsUseCase,
    private val refreshEventsUseCase: RefreshEventsUseCase,
    private val mapper: EventUiMapper,
    private val todayProvider: TodayProvider
) : ViewModel() {

    private val internalState = MutableStateFlow(EventListInternalState())
    private val _uiEvents = MutableSharedFlow<EventListUiEvent>()
    val uiEvents: SharedFlow<EventListUiEvent> = _uiEvents.asSharedFlow()

    val uiState: StateFlow<EventListScreenUiState> =
        combine(observeEventsUseCase.observe(), internalState) { events, internal ->
            val today = todayProvider.today()
            val (finished, upcoming) = events.partition { it.isFinished }
            EventListScreenUiState.Loaded(
                EventListUiState(
                    selectedTab = internal.selectedTab,
                    upcoming = upcoming.sortedWith(compareBy({ it.date }, { it.name })).map { mapper.toCard(it, today) },
                    finished = finished.sortedWith(compareByDescending<Event> { it.date }.thenBy { it.name })
                        .map { mapper.toCard(it, today) },
                    reservedUnits = upcoming.sumOf { it.totalUnits },
                    isRefreshing = internal.isRefreshing,
                    isStale = internal.isStale
                )
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EventListScreenUiState.Loading
        )

    init {
        refresh()
    }

    fun onAction(action: EventListUiAction) {
        when (action) {
            is EventListUiAction.SelectTab -> internalState.update { it.copy(selectedTab = action.tab) }
            EventListUiAction.Refresh -> refresh()
        }
    }

    private fun refresh() {
        if (internalState.value.isRefreshing) return
        viewModelScope.launch {
            internalState.update { it.copy(isRefreshing = true) }
            val result = refreshEventsUseCase.refresh()
            val error = (result as? Result.Failure)?.error
            internalState.update { it.copy(isRefreshing = false, isStale = error == EventsError.NoInternet) }
            // Without coverage the banner says it; anything else is worth a message.
            if (error != null && error != EventsError.NoInternet) _uiEvents.emit(EventListUiEvent.ShowError(error.toUiText()))
        }
    }
}
