package com.granatum.feature.events.presentation.list

import androidx.compose.runtime.Immutable
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.presentation.common.EventCardUiState

@Immutable
internal sealed interface EventListScreenUiState {
    data object Loading : EventListScreenUiState
    data class Loaded(val data: EventListUiState) : EventListScreenUiState
}

internal enum class EventTab { UPCOMING, FINISHED }

@Immutable
internal data class EventListUiState(
    val selectedTab: EventTab,
    /** Soonest first. */
    val upcoming: List<EventCardUiState>,
    /** Most recent first. */
    val finished: List<EventCardUiState>,
    val reservedUnits: Int,
    val isRefreshing: Boolean,
    /** The last refresh failed for lack of coverage: what is shown may be out of date. */
    val isStale: Boolean
) {
    /** The soonest event, shown large on top of the upcoming tab. */
    val nextEvent: EventCardUiState? get() = upcoming.firstOrNull()

    /** The cards of the tab; on the upcoming one, without the event already shown large. */
    val shown: List<EventCardUiState> get() = if (selectedTab == EventTab.UPCOMING) upcoming.drop(1) else finished

    val isEmpty: Boolean get() = if (selectedTab == EventTab.UPCOMING) upcoming.isEmpty() else finished.isEmpty()
}

@Immutable
internal sealed interface EventListUiAction {
    data class SelectTab(val tab: EventTab) : EventListUiAction
    data object Refresh : EventListUiAction
}

internal sealed interface EventListUiEvent {
    data class ShowError(val message: UiText) : EventListUiEvent
}
