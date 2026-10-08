package com.granatum.feature.invoicing.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.invoicing.domain.model.InvoiceHistory
import com.granatum.feature.invoicing.domain.usecase.InvoicingUseCases
import com.granatum.feature.invoicing.presentation.common.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoiceHistoryState(
    val history: InvoiceHistory? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
)

class InvoiceHistoryViewModel(
    private val invoiceId: String,
    private val invoicing: InvoicingUseCases,
) : ViewModel() {
    private val _state = MutableStateFlow(InvoiceHistoryState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = invoicing.repository.history(invoiceId)) {
                is Result.Success -> _state.update { it.copy(history = result.data, isLoading = false) }
                is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }
}
