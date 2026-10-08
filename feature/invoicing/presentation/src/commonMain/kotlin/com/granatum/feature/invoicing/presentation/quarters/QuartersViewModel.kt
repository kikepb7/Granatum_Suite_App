package com.granatum.feature.invoicing.presentation.quarters

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.invoicing.domain.model.FiscalLimits
import com.granatum.feature.invoicing.domain.model.Quarter
import com.granatum.feature.invoicing.domain.usecase.InvoicingUseCases
import com.granatum.feature.invoicing.presentation.common.toUiText
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import granatumsuite.feature.invoicing.presentation.generated.resources.quarter_done_close
import granatumsuite.feature.invoicing.presentation.generated.resources.quarter_done_reopen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuartersState(
    val year: Int,
    val quarters: List<Quarter> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val closing: Quarter? = null,
    val reopening: Quarter? = null,
    val reasonInvalid: Boolean = false,
    val isBusy: Boolean = false,
    val message: UiText? = null,
)

sealed interface QuartersAction {
    data class OnYear(
        val delta: Int,
    ) : QuartersAction

    data class OnClose(
        val quarter: Quarter,
    ) : QuartersAction

    data object OnCloseConfirm : QuartersAction

    data class OnReopen(
        val quarter: Quarter,
    ) : QuartersAction

    data object OnReopenConfirm : QuartersAction

    data object OnDismiss : QuartersAction

    data object OnRetry : QuartersAction

    data object OnMessageShown : QuartersAction
}

class QuartersViewModel(
    private val invoicing: InvoicingUseCases,
    currentYear: Int,
) : ViewModel() {
    val reason = TextFieldState()
    private val _state = MutableStateFlow(QuartersState(year = currentYear))
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: QuartersAction) {
        when (action) {
            is QuartersAction.OnYear -> {
                _state.update { it.copy(year = it.year + action.delta) }
                load()
            }
            is QuartersAction.OnClose -> _state.update { it.copy(closing = action.quarter) }
            QuartersAction.OnCloseConfirm -> close()
            is QuartersAction.OnReopen -> {
                reason.clearText()
                _state.update { it.copy(reopening = action.quarter, reasonInvalid = false) }
            }
            QuartersAction.OnReopenConfirm -> reopen()
            QuartersAction.OnDismiss -> _state.update { it.copy(closing = null, reopening = null) }
            QuartersAction.OnRetry -> load()
            QuartersAction.OnMessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val year = _state.value.year
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = invoicing.repository.quarters(year)) {
                is Result.Success -> _state.update { if (it.year == year) it.copy(quarters = result.data, isLoading = false) else it }
                is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun close() {
        val quarter = _state.value.closing ?: return
        _state.update { it.copy(closing = null, isBusy = true) }
        viewModelScope.launch {
            val result = invoicing.repository.closeQuarter(quarter.year, quarter.quarter)
            apply(result, Res.string.quarter_done_close)
        }
    }

    private fun reopen() {
        val quarter = _state.value.reopening ?: return
        val text = reason.text.toString().trim()
        if (text.length !in FiscalLimits.REOPEN_REASON_MIN..FiscalLimits.REOPEN_REASON_MAX) {
            _state.update { it.copy(reasonInvalid = true) }
            return
        }
        _state.update { it.copy(reopening = null, isBusy = true, reasonInvalid = false) }
        viewModelScope.launch {
            val result = invoicing.repository.reopenQuarter(quarter.year, quarter.quarter, text)
            apply(result, Res.string.quarter_done_reopen)
        }
    }

    private fun apply(
        result: Result<Quarter, com.granatum.feature.invoicing.domain.model.InvoicingError>,
        done: org.jetbrains.compose.resources.StringResource,
    ) {
        when (result) {
            is Result.Success ->
                _state.update { state ->
                    state.copy(
                        isBusy = false,
                        quarters = state.quarters.map { if (it.quarter == result.data.quarter) result.data else it },
                        message = UiText.Resource(done),
                    )
                }
            is Result.Failure -> _state.update { it.copy(isBusy = false, message = result.error.toUiText()) }
        }
    }
}
