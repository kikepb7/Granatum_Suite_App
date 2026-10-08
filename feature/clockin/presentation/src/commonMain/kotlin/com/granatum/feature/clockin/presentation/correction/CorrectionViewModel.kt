package com.granatum.feature.clockin.presentation.correction

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.ClockEventType
import com.granatum.feature.clockin.domain.model.CorrectionDraft
import com.granatum.feature.clockin.domain.model.CorrectionIssue
import com.granatum.feature.clockin.domain.model.CorrectionValidation
import com.granatum.feature.clockin.domain.model.CorrectionValues
import com.granatum.feature.clockin.domain.model.ProposedBreak
import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.model.SyncState
import com.granatum.feature.clockin.domain.usecase.ObserveShiftUseCase
import com.granatum.feature.clockin.domain.usecase.RequestCorrectionUseCase
import com.granatum.feature.clockin.presentation.common.timeLabel
import com.granatum.feature.clockin.presentation.mapper.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class BreakFields(val type: BreakType, val start: TextFieldState, val end: TextFieldState)

data class CorrectionUiState(
    val breaks: List<BreakFields> = emptyList(),
    val issues: Set<CorrectionIssue> = emptySet(),
    val badTime: Boolean = false,
    val error: UiText? = null,
    val isSending: Boolean = false,
    val ready: Boolean = false
)

sealed interface CorrectionAction {
    data class OnAddBreak(val type: BreakType) : CorrectionAction
    data class OnRemoveBreak(val index: Int) : CorrectionAction
    data object OnSubmit : CorrectionAction
}

sealed interface CorrectionEvent {
    data object Sent : CorrectionEvent
}

/**
 * Asks for a closed shift to be corrected (spec 005, US5). Starts from the shift as it stands,
 * so the person only changes what is wrong. Times are typed as HH:MM on the shift's own day; a
 * shift crossing midnight is out of reach of this screen, and the server's own validation is
 * the backstop.
 */
class CorrectionViewModel(
    private val shiftServerId: String,
    observeShift: ObserveShiftUseCase,
    private val requestCorrection: RequestCorrectionUseCase,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ViewModel() {

    val entry = TextFieldState()
    val exit = TextFieldState()
    val reason = TextFieldState()

    private val _state = MutableStateFlow(CorrectionUiState())
    val state: StateFlow<CorrectionUiState> = _state.asStateFlow()
    private val eventChannel = Channel<CorrectionEvent>()
    val events = eventChannel.receiveAsFlow()

    private var day: LocalDate? = null

    init {
        viewModelScope.launch { prefill(observeShift(shiftServerId).filterNotNull().first()) }
    }

    private fun prefill(shift: ShiftModel) {
        day = shift.start.toLocalDateTime(timeZone).date
        entry.edit { replace(0, length, shift.start.timeLabel(timeZone)) }
        shift.end?.let { end -> exit.edit { replace(0, length, end.timeLabel(timeZone)) } }
        val counted = shift.items.filter { it.syncState != SyncState.REJECTED }
        val breaks = counted.filter { it.type == ClockEventType.BREAK_START }.map { start ->
            val end = counted.firstOrNull { it.type == ClockEventType.BREAK_END && it.at >= start.at }
            BreakFields(start.breakType ?: BreakType.OTRO, TextFieldState(start.at.timeLabel(timeZone)), TextFieldState(end?.at?.timeLabel(timeZone) ?: ""))
        }
        _state.update { it.copy(breaks = breaks, ready = true) }
    }

    fun onAction(action: CorrectionAction) {
        when (action) {
            is CorrectionAction.OnAddBreak -> _state.update { it.copy(breaks = it.breaks + BreakFields(action.type, TextFieldState(), TextFieldState())) }
            is CorrectionAction.OnRemoveBreak -> _state.update { it.copy(breaks = it.breaks.filterIndexed { i, _ -> i != action.index }) }
            CorrectionAction.OnSubmit -> submit()
        }
    }

    private fun submit() {
        if (_state.value.isSending) return
        val values = buildValues() ?: run {
            _state.update { it.copy(badTime = true, issues = emptySet(), error = null) }
            return
        }
        val draft = CorrectionDraft(reason.text.toString(), values)
        val issues = CorrectionValidation.validate(draft)
        if (issues.isNotEmpty()) {
            _state.update { it.copy(issues = issues, badTime = false, error = null) }
            return
        }
        _state.update { it.copy(isSending = true, issues = emptySet(), badTime = false, error = null) }
        viewModelScope.launch {
            when (val result = requestCorrection(shiftServerId, draft)) {
                is Result.Success -> {
                    _state.update { it.copy(isSending = false) }
                    eventChannel.send(CorrectionEvent.Sent)
                }
                is Result.Failure -> _state.update { it.copy(isSending = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun buildValues(): CorrectionValues? {
        val d = day ?: return null
        val entryAt = parse(d, entry.text.toString()) ?: return null
        val exitAt = parse(d, exit.text.toString()) ?: return null
        val breaks = _state.value.breaks.map { b ->
            ProposedBreak(b.type, parse(d, b.start.text.toString()) ?: return null, parse(d, b.end.text.toString()) ?: return null)
        }
        return CorrectionValues(entryAt, exitAt, breaks)
    }

    private fun parse(day: LocalDate, text: String): Instant? {
        val match = TIME.matchEntire(text.trim()) ?: return null
        val (h, m) = match.destructured
        val time = runCatching { LocalTime(h.toInt(), m.toInt()) }.getOrNull() ?: return null
        return day.atTime(time).toInstant(timeZone)
    }

    private companion object {
        val TIME = Regex("""^(\d{1,2}):(\d{2})$""")
    }
}
