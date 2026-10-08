package com.granatum.feature.clockin.presentation.clockin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.model.TodayState
import com.granatum.feature.clockin.domain.usecase.ClockInUseCase
import com.granatum.feature.clockin.domain.usecase.ClockOutUseCase
import com.granatum.feature.clockin.domain.usecase.EndBreakUseCase
import com.granatum.feature.clockin.domain.usecase.ObserveTodayUseCase
import com.granatum.feature.clockin.domain.usecase.RefreshMonthUseCase
import com.granatum.feature.clockin.domain.usecase.StartBreakUseCase
import com.granatum.feature.clockin.presentation.mapper.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class ClockInUiState(
    val today: TodayState = TodayState(),
    val isProcessing: Boolean = false,
    val isChoosingBreak: Boolean = false
) {
    val status: ShiftStatus get() = today.status

    /** FR-004: an open break has to end before clocking out. */
    val canClockOut: Boolean get() = status == ShiftStatus.CLOCKED_IN
}

sealed interface ClockInAction {
    data object OnPrimaryButtonClick : ClockInAction
    data object OnBreakToggleClick : ClockInAction
    data class OnBreakTypeChosen(val type: BreakType) : ClockInAction
    data object OnBreakSheetDismissed : ClockInAction
    data class OnRequestCorrection(val shiftKey: String) : ClockInAction
}

sealed interface ClockInEvent {
    data class Error(val message: UiText) : ClockInEvent
    data class OpenShift(val shiftKey: String) : ClockInEvent
}

class ClockInViewModel(
    observeToday: ObserveTodayUseCase,
    private val refreshMonth: RefreshMonthUseCase,
    private val clockIn: ClockInUseCase,
    private val clockOut: ClockOutUseCase,
    private val startBreak: StartBreakUseCase,
    private val endBreak: EndBreakUseCase,
    private val clock: Clock = Clock.System
) : ViewModel() {

    private val ui = MutableStateFlow(ClockInUiState())
    private val eventChannel = Channel<ClockInEvent>()
    val events = eventChannel.receiveAsFlow()

    val state = combine(observeToday(), ui) { today, ui -> ui.copy(today = today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ClockInUiState())

    init {
        // What another device did today should show up here too (FR-018). Silent without coverage.
        viewModelScope.launch {
            val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            refreshMonth(today.year, today.month.number)
        }
    }

    fun onAction(action: ClockInAction) {
        when (action) {
            ClockInAction.OnPrimaryButtonClick -> when (state.value.status) {
                ShiftStatus.CLOCKED_OUT -> run { clockIn() }
                ShiftStatus.CLOCKED_IN -> run { clockOut() }
                ShiftStatus.ON_BREAK -> Unit // the button is disabled; nothing to do
            }
            ClockInAction.OnBreakToggleClick -> when (state.value.status) {
                ShiftStatus.CLOCKED_IN -> ui.update { it.copy(isChoosingBreak = true) }
                ShiftStatus.ON_BREAK -> run { endBreak() }
                ShiftStatus.CLOCKED_OUT -> Unit
            }
            is ClockInAction.OnBreakTypeChosen -> {
                ui.update { it.copy(isChoosingBreak = false) }
                run { startBreak(action.type) }
            }
            ClockInAction.OnBreakSheetDismissed -> ui.update { it.copy(isChoosingBreak = false) }
            is ClockInAction.OnRequestCorrection -> viewModelScope.launch { eventChannel.send(ClockInEvent.OpenShift(action.shiftKey)) }
        }
    }

    private fun run(action: suspend () -> Result<Unit, ClockActionError>) {
        if (ui.value.isProcessing) return
        viewModelScope.launch {
            ui.update { it.copy(isProcessing = true) }
            val result = action()
            if (result is Result.Failure) eventChannel.send(ClockInEvent.Error(result.error.toUiText()))
            ui.update { it.copy(isProcessing = false) }
        }
    }
}
