package com.granatum.feature.invoicing.presentation.company

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.invoicing.domain.model.FiscalLimits
import com.granatum.feature.invoicing.domain.model.InvoicingError
import com.granatum.feature.invoicing.domain.usecase.InvoicingUseCases
import com.granatum.feature.invoicing.presentation.common.toUiText
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import granatumsuite.feature.invoicing.presentation.generated.resources.company_saved
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CompanyIssue { NAME_REQUIRED, NAME_TOO_LONG, TAX_ID_REQUIRED, TAX_ID_TOO_LONG, TAX_ID_INVALID }

data class CompanyState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val recognitionEnabled: Boolean? = null,
    val issues: Set<CompanyIssue> = emptySet(),
    val isSaving: Boolean = false,
    val message: UiText? = null,
)

class CompanyViewModel(
    private val invoicing: InvoicingUseCases,
) : ViewModel() {
    val legalName = TextFieldState()
    val taxId = TextFieldState()
    private val _state = MutableStateFlow(CompanyState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun messageShown() = _state.update { it.copy(message = null) }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            when (val result = invoicing.repository.company()) {
                is Result.Success -> {
                    result.data?.let {
                        legalName.setTextAndPlaceCursorAtEnd(it.legalName)
                        taxId.setTextAndPlaceCursorAtEnd(it.taxId)
                    }
                    _state.update { it.copy(isLoading = false, recognitionEnabled = result.data?.recognitionEnabled) }
                }
                is Result.Failure -> _state.update { it.copy(isLoading = false, loadError = result.error.toUiText()) }
            }
        }
    }

    fun save() {
        if (_state.value.isSaving) return
        val name = legalName.text.toString().trim()
        val id = taxId.text.toString().trim()
        val issues =
            buildSet {
                if (name.isEmpty()) add(CompanyIssue.NAME_REQUIRED)
                if (name.length > FiscalLimits.COMPANY_NAME) add(CompanyIssue.NAME_TOO_LONG)
                if (id.isEmpty()) add(CompanyIssue.TAX_ID_REQUIRED)
                if (id.length > FiscalLimits.TAX_ID) add(CompanyIssue.TAX_ID_TOO_LONG)
            }
        if (issues.isNotEmpty()) {
            _state.update { it.copy(issues = issues) }
            return
        }
        _state.update { it.copy(isSaving = true, issues = emptySet()) }
        viewModelScope.launch {
            when (val result = invoicing.repository.saveCompany(name, id)) {
                is Result.Success -> {
                    // The server normalises the NIF (upper case, no spaces): show it as stored.
                    taxId.setTextAndPlaceCursorAtEnd(result.data.taxId)
                    _state.update {
                        it.copy(
                            isSaving = false,
                            recognitionEnabled = result.data.recognitionEnabled,
                            message = UiText.Resource(Res.string.company_saved),
                        )
                    }
                }
                is Result.Failure ->
                    _state.update {
                        if (result.error == InvoicingError.InvalidTaxId) {
                            it.copy(isSaving = false, issues = setOf(CompanyIssue.TAX_ID_INVALID))
                        } else {
                            it.copy(isSaving = false, message = result.error.toUiText())
                        }
                    }
            }
        }
    }
}
