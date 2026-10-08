package com.granatum.feature.invoicing.presentation.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.invoicing.domain.model.InvoicingFile
import com.granatum.feature.invoicing.domain.model.Report
import com.granatum.feature.invoicing.domain.model.ReportFormat
import com.granatum.feature.invoicing.domain.model.ReportPeriod
import com.granatum.feature.invoicing.domain.usecase.InvoicingUseCases
import com.granatum.feature.invoicing.presentation.common.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PeriodType { MONTHLY, QUARTERLY, YEARLY }

data class ReportState(
    val type: PeriodType = PeriodType.QUARTERLY,
    val year: Int,
    val month: Int,
    val quarter: Int,
    val report: Report? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val downloading: ReportFormat? = null,
    val message: UiText? = null,
) {
    val period: ReportPeriod get() =
        when (type) {
            PeriodType.MONTHLY -> ReportPeriod.Monthly(year, month)
            PeriodType.QUARTERLY -> ReportPeriod.Quarterly(year, quarter)
            PeriodType.YEARLY -> ReportPeriod.Yearly(year)
        }
}

sealed interface ReportAction {
    data class OnType(
        val type: PeriodType,
    ) : ReportAction

    /** Moves the period back or forward by one of its own units. */
    data class OnStep(
        val delta: Int,
    ) : ReportAction

    data class OnDownload(
        val format: ReportFormat,
    ) : ReportAction

    data object OnRetry : ReportAction

    data object OnMessageShown : ReportAction
}

class ReportViewModel(
    private val invoicing: InvoicingUseCases,
    year: Int,
    month: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(ReportState(year = year, month = month, quarter = (month - 1) / 3 + 1))
    val state = _state.asStateFlow()
    private val files = Channel<InvoicingFile>()

    /** A downloaded report for the screen to open or share. */
    val fileEvents = files.receiveAsFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: ReportAction) {
        when (action) {
            is ReportAction.OnType -> {
                _state.update { it.copy(type = action.type) }
                load()
            }
            is ReportAction.OnStep -> {
                _state.update { it.step(action.delta) }
                load()
            }
            is ReportAction.OnDownload -> download(action.format)
            ReportAction.OnRetry -> load()
            ReportAction.OnMessageShown -> _state.update { it.copy(message = null) }
        }
    }

    /** Month and quarter move together, so switching the period type stays in the same season. */
    private fun ReportState.step(delta: Int): ReportState =
        when (type) {
            PeriodType.YEARLY -> copy(year = year + delta)
            PeriodType.QUARTERLY -> {
                val index = year * 4 + (quarter - 1) + delta
                val newQuarter = index % 4 + 1
                copy(year = index / 4, quarter = newQuarter, month = (newQuarter - 1) * 3 + 1)
            }
            PeriodType.MONTHLY -> {
                val index = year * 12 + (month - 1) + delta
                val newMonth = index % 12 + 1
                copy(year = index / 12, month = newMonth, quarter = (newMonth - 1) / 3 + 1)
            }
        }

    private fun load() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                val period = _state.value.period
                _state.update { it.copy(isLoading = true, error = null, report = null) }
                when (val result = invoicing.repository.report(period)) {
                    is Result.Success -> _state.update { it.copy(report = result.data, isLoading = false) }
                    is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                }
            }
    }

    private fun download(format: ReportFormat) {
        if (_state.value.downloading != null) return
        viewModelScope.launch {
            _state.update { it.copy(downloading = format) }
            when (val result = invoicing.repository.reportFile(_state.value.period, format)) {
                is Result.Success -> files.send(result.data)
                is Result.Failure -> _state.update { it.copy(message = result.error.toUiText()) }
            }
            _state.update { it.copy(downloading = null) }
        }
    }
}
