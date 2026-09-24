package com.granatum.feature.clockin.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.feature.clockin.domain.model.DailyAttendanceSummary
import com.granatum.feature.clockin.domain.usecase.GetAttendanceHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

private const val HISTORY_WINDOW_DAYS = 14

class AttendanceHistoryViewModel(
    getAttendanceHistoryUseCase: GetAttendanceHistoryUseCase
) : ViewModel() {

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val range = MutableStateFlow(today.minus(HISTORY_WINDOW_DAYS, DateTimeUnit.DAY) to today)

    val state = range.flatMapLatest { (from, to) ->
        getAttendanceHistoryUseCase(from = from, to = to)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )
}
