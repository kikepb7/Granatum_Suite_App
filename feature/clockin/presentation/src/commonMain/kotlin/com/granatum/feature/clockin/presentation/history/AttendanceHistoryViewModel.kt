package com.granatum.feature.clockin.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.usecase.ObserveMonthUseCase
import com.granatum.feature.clockin.domain.usecase.RefreshMonthUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class YearMonth(val year: Int, val month: Int) {
    fun previous() = if (month == 1) YearMonth(year - 1, 12) else YearMonth(year, month - 1)
    fun next() = if (month == 12) YearMonth(year + 1, 1) else YearMonth(year, month + 1)
    operator fun compareTo(other: YearMonth) = compareValuesBy(this, other, { it.year }, { it.month })
}

data class HistoryUiState(
    val month: YearMonth,
    val shifts: List<ShiftModel> = emptyList(),
    val canGoNext: Boolean = false
)

sealed interface HistoryAction {
    data object OnPreviousMonth : HistoryAction
    data object OnNextMonth : HistoryAction
}

/** The month as the server has it, with what is still pending on top (spec 005, US3). */
@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceHistoryViewModel(
    private val observeMonth: ObserveMonthUseCase,
    private val refreshMonth: RefreshMonthUseCase,
    clock: Clock = Clock.System
) : ViewModel() {

    private val current = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.let { YearMonth(it.year, it.month.number) }
    private val month = MutableStateFlow(current)

    val state = month.flatMapLatest { m ->
        observeMonth(m.year, m.month).map { shifts -> HistoryUiState(m, shifts.sortedByDescending { it.start }, m < current) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), HistoryUiState(current))

    init {
        refresh(current)
    }

    fun onAction(action: HistoryAction) {
        when (action) {
            HistoryAction.OnPreviousMonth -> month.update { it.previous() }.also { refresh(month.value) }
            HistoryAction.OnNextMonth -> if (month.value < current) month.update { it.next() }.also { refresh(month.value) }
        }
    }

    /** Silent without coverage: the last download is shown instead (FR-016). */
    private fun refresh(m: YearMonth) {
        viewModelScope.launch { refreshMonth(m.year, m.month) }
    }
}
