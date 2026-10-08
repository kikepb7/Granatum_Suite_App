package com.granatum.feature.invoicing.presentation.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.invoicing.domain.model.FiscalLimits
import com.granatum.feature.invoicing.domain.model.InvoiceFilter
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceSummary
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.UploadDocument
import com.granatum.feature.invoicing.domain.model.UploadOutcome
import com.granatum.feature.invoicing.domain.usecase.InvoicingUseCases
import com.granatum.feature.invoicing.presentation.common.toUiText
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class UploadItem(
    val fileName: String,
    val outcome: UploadOutcome,
    val invoiceId: String?,
)

data class InvoiceListState(
    val items: List<InvoiceSummary> = emptyList(),
    val filter: InvoiceFilter = InvoiceFilter(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val page: Int = 0,
    /** The first page failed: nothing to show but the reason and a retry. */
    val error: UiText? = null,
    val loadMoreFailed: Boolean = false,
    val companyMissing: Boolean = false,
    val uploadingCount: Int = 0,
    val uploadResults: List<UploadItem>? = null,
    val message: UiText? = null,
) {
    val isFiltered: Boolean get() = filter != InvoiceFilter()
}

sealed interface InvoiceListAction {
    data class OnState(
        val state: InvoiceState?,
    ) : InvoiceListAction

    data class OnType(
        val type: InvoiceType?,
    ) : InvoiceListAction

    data class OnDates(
        val from: LocalDate?,
        val to: LocalDate?,
    ) : InvoiceListAction

    data object OnRefresh : InvoiceListAction

    data object OnLoadMore : InvoiceListAction

    data class OnUpload(
        val documents: List<UploadDocument>,
    ) : InvoiceListAction

    data object OnDismissUploadResults : InvoiceListAction

    data object OnMessageShown : InvoiceListAction
}

/**
 * Pages of 50, newest first as the server sorts them (undated ones, that is pending recognition,
 * on top). While pending invoices are shown, the first page is polled so they turn into drafts on
 * their own (research D6): every [pollIntervalMillis], at most [maxPolls] times after each upload
 * or refresh.
 */
@OptIn(FlowPreview::class)
class InvoiceListViewModel(
    private val invoicing: InvoicingUseCases,
    private val pollIntervalMillis: Long = 5_000L,
    private val maxPolls: Int = 24,
) : ViewModel() {
    val query = TextFieldState()
    private val _state = MutableStateFlow(InvoiceListState())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null
    private var pollJob: Job? = null

    init {
        reload()
        checkCompany()
        viewModelScope.launch {
            snapshotFlow { query.text.toString() }
                .drop(1)
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                .collect { text -> setFilter(_state.value.filter.copy(text = text)) }
        }
    }

    fun onAction(action: InvoiceListAction) {
        when (action) {
            is InvoiceListAction.OnState -> setFilter(_state.value.filter.copy(state = action.state))
            is InvoiceListAction.OnType -> setFilter(_state.value.filter.copy(type = action.type))
            is InvoiceListAction.OnDates -> setFilter(_state.value.filter.copy(from = action.from, to = action.to))
            InvoiceListAction.OnRefresh -> {
                reload()
                checkCompany()
            }
            InvoiceListAction.OnLoadMore -> loadMore()
            is InvoiceListAction.OnUpload -> upload(action.documents)
            InvoiceListAction.OnDismissUploadResults -> _state.update { it.copy(uploadResults = null) }
            InvoiceListAction.OnMessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun setFilter(filter: InvoiceFilter) {
        if (filter == _state.value.filter) return
        _state.update { it.copy(filter = filter) }
        reload()
    }

    private fun reload() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, error = null, loadMoreFailed = false) }
                when (val result = invoicing.repository.invoices(_state.value.filter, page = 0)) {
                    is Result.Success ->
                        _state.update {
                            it.copy(items = result.data.items, page = 0, hasMore = result.data.hasMore, isLoading = false)
                        }
                    is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
                }
                startPollingIfPending()
            }
    }

    private fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
            when (val result = invoicing.repository.invoices(current.filter, page = current.page + 1)) {
                is Result.Success ->
                    _state.update { state ->
                        val known = state.items.map { it.id }.toSet()
                        state.copy(
                            items = state.items + result.data.items.filterNot { it.id in known },
                            page = result.data.page,
                            hasMore = result.data.hasMore,
                            isLoadingMore = false,
                        )
                    }
                is Result.Failure -> _state.update { it.copy(isLoadingMore = false, loadMoreFailed = true) }
            }
        }
    }

    private fun startPollingIfPending() {
        if (_state.value.items.none { it.state == InvoiceState.PENDING_RECOGNITION }) return
        pollJob?.cancel()
        pollJob =
            viewModelScope.launch {
                repeat(maxPolls) {
                    delay(pollIntervalMillis)
                    val result = invoicing.repository.invoices(_state.value.filter, page = 0)
                    if (result is Result.Success) {
                        _state.update { state -> state.copy(items = merge(state.items, result.data.items)) }
                    }
                    if (_state.value.items.none { it.state == InvoiceState.PENDING_RECOGNITION }) return@launch
                }
            }
    }

    /** Fresh first-page rows replace the ones already shown; new ones go on top. */
    private fun merge(
        current: List<InvoiceSummary>,
        fresh: List<InvoiceSummary>,
    ): List<InvoiceSummary> {
        val byId = fresh.associateBy { it.id }
        val known = current.map { it.id }.toSet()
        return fresh.filterNot { it.id in known } + current.map { byId[it.id] ?: it }
    }

    /**
     * Files over the per-file cap are reported without sending them; the rest go in batches under
     * the per-request cap, so one big selection never fails as a whole.
     */
    private fun upload(documents: List<UploadDocument>) {
        if (documents.isEmpty() || _state.value.uploadingCount > 0) return
        viewModelScope.launch {
            _state.update { it.copy(uploadingCount = documents.size) }
            val results = arrayOfNulls<UploadItem>(documents.size)
            val sendable =
                documents.withIndex().filter { (index, document) ->
                    val tooBig = document.bytes.size > FiscalLimits.MAX_FILE_BYTES
                    if (tooBig) results[index] = UploadItem(document.fileName, UploadOutcome.TOO_LARGE, null)
                    !tooBig
                }
            var failure: UiText? = null
            for (batch in batches(sendable)) {
                when (val result = invoicing.repository.upload(batch.map { it.value })) {
                    is Result.Success ->
                        result.data.forEach { uploaded ->
                            batch.getOrNull(uploaded.position - 1)?.let { (index, document) ->
                                results[index] = UploadItem(document.fileName, uploaded.outcome, uploaded.invoiceId)
                            }
                        }
                    is Result.Failure -> {
                        failure = result.error.toUiText()
                        break
                    }
                }
            }
            val done = results.filterNotNull()
            _state.update {
                it.copy(uploadingCount = 0, uploadResults = done.ifEmpty { null }, message = failure)
            }
            if (done.any { item -> item.outcome == UploadOutcome.ACCEPTED }) reload()
        }
    }

    private fun batches(documents: List<IndexedValue<UploadDocument>>): List<List<IndexedValue<UploadDocument>>> {
        val batches = mutableListOf<MutableList<IndexedValue<UploadDocument>>>()
        var size = 0L
        documents.forEach { document ->
            val bytes =
                document.value.bytes.size
                    .toLong()
            if (batches.isEmpty() || size + bytes > FiscalLimits.MAX_REQUEST_BYTES) {
                batches += mutableListOf<IndexedValue<UploadDocument>>()
                size = 0
            }
            batches.last() += document
            size += bytes
        }
        return batches
    }

    private fun checkCompany() {
        viewModelScope.launch {
            val company = invoicing.repository.company()
            if (company is Result.Success) _state.update { it.copy(companyMissing = company.data == null) }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 400L
    }
}
