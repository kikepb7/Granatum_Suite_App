package com.granatum.feature.clockin.presentation.clockin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result.Failure
import com.granatum.core.domain.util.Result.Success
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.clockin.domain.model.ClockEventModel
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.usecase.ClockInUseCase
import com.granatum.feature.clockin.domain.usecase.ClockOutUseCase
import com.granatum.feature.clockin.domain.usecase.EndBreakUseCase
import com.granatum.feature.clockin.domain.usecase.ObservePendingSyncCountUseCase
import com.granatum.feature.clockin.domain.usecase.ObserveShiftStatusUseCase
import com.granatum.feature.clockin.domain.usecase.ObserveTodayEventsUseCase
import com.granatum.feature.clockin.domain.usecase.StartBreakUseCase
import com.granatum.feature.clockin.presentation.mapper.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClockInViewModel(
    private val observeShiftStatusUseCase: ObserveShiftStatusUseCase,
    private val observeTodayEventsUseCase: ObserveTodayEventsUseCase,
    private val observePendingSyncCountUseCase: ObservePendingSyncCountUseCase,
    private val clockInUseCase: ClockInUseCase,
    private val clockOutUseCase: ClockOutUseCase,
    private val startBreakUseCase: StartBreakUseCase,
    private val endBreakUseCase: EndBreakUseCase
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)

    private val eventChannel = Channel<ClockInEvent>()
    val events = eventChannel.receiveAsFlow()

    val state = combine(
        observeShiftStatusUseCase(),
        observeTodayEventsUseCase(),
        observePendingSyncCountUseCase(),
        _isProcessing
    ) { status, todayEvents, pendingCount, isProcessing ->
        ClockInUiState(
            status = status,
            todayEvents = todayEvents,
            pendingSyncCount = pendingCount,
            isProcessing = isProcessing
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ClockInUiState()
        )

    fun onAction(action: ClockInAction) {
        when (action) {
            ClockInAction.OnPrimaryButtonClick -> onPrimaryButtonClick()
            ClockInAction.OnBreakToggleClick -> onBreakToggleClick()
        }
    }

    private fun onPrimaryButtonClick() {
        when (state.value.status) {
            ShiftStatus.CLOCKED_OUT -> run(clockInUseCase::invoke)
            ShiftStatus.CLOCKED_IN, ShiftStatus.ON_BREAK -> run(clockOutUseCase::invoke)
        }
    }

    private fun onBreakToggleClick() {
        when (state.value.status) {
            ShiftStatus.CLOCKED_IN -> run(startBreakUseCase::invoke)
            ShiftStatus.ON_BREAK -> run(endBreakUseCase::invoke)
            ShiftStatus.CLOCKED_OUT -> Unit
        }
    }

    private fun run(action: suspend () -> com.granatum.core.domain.util.Result<Unit, com.granatum.feature.clockin.domain.model.ClockActionError>) {
        viewModelScope.launch {
            _isProcessing.update { true }
            when (val result = action()) {
                is Success -> Unit
                is Failure -> eventChannel.send(ClockInEvent.Error(result.error.toUiText()))
            }
            _isProcessing.update { false }
        }
    }
}

data class ClockInUiState(
    val status: ShiftStatus = ShiftStatus.CLOCKED_OUT,
    val todayEvents: List<ClockEventModel> = emptyList(),
    val pendingSyncCount: Int = 0,
    val isProcessing: Boolean = false
)

sealed interface ClockInAction {
    data object OnPrimaryButtonClick : ClockInAction
    data object OnBreakToggleClick : ClockInAction
}

sealed interface ClockInEvent {
    data class Error(val message: UiText) : ClockInEvent
}
