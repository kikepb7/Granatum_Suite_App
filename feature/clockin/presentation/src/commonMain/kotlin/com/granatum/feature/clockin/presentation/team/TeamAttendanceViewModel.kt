package com.granatum.feature.clockin.presentation.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result.Failure
import com.granatum.core.domain.util.Result.Success
import com.granatum.core.presentation.mapper.toUiText
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.clockin.domain.model.CorrectionRequestModel
import com.granatum.feature.clockin.domain.model.EmployeeAttendanceSummaryModel
import com.granatum.feature.clockin.domain.usecase.ApproveCorrectionUseCase
import com.granatum.feature.clockin.domain.usecase.GetPendingCorrectionsUseCase
import com.granatum.feature.clockin.domain.usecase.GetTeamAttendanceUseCase
import com.granatum.feature.clockin.domain.usecase.RefreshTeamAttendanceUseCase
import com.granatum.feature.clockin.domain.usecase.RejectCorrectionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

private const val PERIOD_WINDOW_DAYS = 7

class TeamAttendanceViewModel(
    private val getTeamAttendanceUseCase: GetTeamAttendanceUseCase,
    private val getPendingCorrectionsUseCase: GetPendingCorrectionsUseCase,
    private val refreshTeamAttendanceUseCase: RefreshTeamAttendanceUseCase,
    private val approveCorrectionUseCase: ApproveCorrectionUseCase,
    private val rejectCorrectionUseCase: RejectCorrectionUseCase
) : ViewModel() {

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val from = today.minus(PERIOD_WINDOW_DAYS, DateTimeUnit.DAY)

    private var hasLoadedInitialData = false
    private val _isRefreshing = MutableStateFlow(false)

    private val eventChannel = Channel<TeamAttendanceEvent>()
    val events = eventChannel.receiveAsFlow()

    val state = combine(
        getTeamAttendanceUseCase(from = from, to = today),
        getPendingCorrectionsUseCase(),
        _isRefreshing
    ) { summaries, corrections, isRefreshing ->
        TeamAttendanceUiState(summaries = summaries, corrections = corrections, isRefreshing = isRefreshing)
    }
        .onStart { if (!hasLoadedInitialData) { refresh(); hasLoadedInitialData = true } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TeamAttendanceUiState()
        )

    fun onAction(action: TeamAttendanceAction) {
        when (action) {
            TeamAttendanceAction.OnRefresh -> refresh()
            is TeamAttendanceAction.OnApproveCorrection -> approve(action.correctionId)
            is TeamAttendanceAction.OnRejectCorrection -> reject(action.correctionId)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _isRefreshing.update { true }
            when (val result = refreshTeamAttendanceUseCase(from = from, to = today)) {
                is Success -> Unit
                is Failure -> eventChannel.send(TeamAttendanceEvent.Error(result.error.toUiText()))
            }
            _isRefreshing.update { false }
        }
    }

    private fun approve(correctionId: String) {
        viewModelScope.launch {
            when (val result = approveCorrectionUseCase(correctionId)) {
                is Success -> Unit
                is Failure -> eventChannel.send(TeamAttendanceEvent.Error(result.error.toUiText()))
            }
        }
    }

    private fun reject(correctionId: String) {
        viewModelScope.launch {
            when (val result = rejectCorrectionUseCase(correctionId, "")) {
                is Success -> Unit
                is Failure -> eventChannel.send(TeamAttendanceEvent.Error(result.error.toUiText()))
            }
        }
    }
}

data class TeamAttendanceUiState(
    val summaries: List<EmployeeAttendanceSummaryModel> = emptyList(),
    val corrections: List<CorrectionRequestModel> = emptyList(),
    val isRefreshing: Boolean = false
)

sealed interface TeamAttendanceAction {
    data object OnRefresh : TeamAttendanceAction
    data class OnApproveCorrection(val correctionId: String) : TeamAttendanceAction
    data class OnRejectCorrection(val correctionId: String) : TeamAttendanceAction
}

sealed interface TeamAttendanceEvent {
    data class Error(val message: UiText) : TeamAttendanceEvent
}
