package com.granatum.feature.invoicing.presentation.detail

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.invoicing.domain.model.Invoice
import com.granatum.feature.invoicing.domain.model.InvoiceDraft
import com.granatum.feature.invoicing.domain.model.InvoiceState
import com.granatum.feature.invoicing.domain.model.InvoiceType
import com.granatum.feature.invoicing.domain.model.InvoicingError
import com.granatum.feature.invoicing.domain.model.Money
import com.granatum.feature.invoicing.domain.model.NoQuotaCause
import com.granatum.feature.invoicing.domain.usecase.InvoicingUseCases
import com.granatum.feature.invoicing.presentation.common.toUiText
import granatumsuite.feature.invoicing.presentation.generated.resources.Res
import granatumsuite.feature.invoicing.presentation.generated.resources.confirmed
import granatumsuite.feature.invoicing.presentation.generated.resources.discarded
import granatumsuite.feature.invoicing.presentation.generated.resources.fix_blocking
import granatumsuite.feature.invoicing.presentation.generated.resources.fix_fields
import granatumsuite.feature.invoicing.presentation.generated.resources.recognizing
import granatumsuite.feature.invoicing.presentation.generated.resources.reloaded
import granatumsuite.feature.invoicing.presentation.generated.resources.saved
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class InvoiceDetailState(
    val invoice: Invoice? = null,
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val type: InvoiceType? = null,
    val issueDate: LocalDate? = null,
    val corrective: Boolean = false,
    val lineKeys: List<Long> = emptyList(),
    val causes: Map<Long, NoQuotaCause?> = emptyMap(),
    val badFields: Set<String> = emptySet(),
    val recognitionEnabled: Boolean = false,
    val isBusy: Boolean = false,
    val confirmingDiscard: Boolean = false,
    val confirmingRecognize: Boolean = false,
    val message: UiText? = null,
) {
    val canEdit: Boolean get() = invoice?.canEdit == true && !isBusy
}

sealed interface InvoiceDetailAction {
    data class OnType(
        val type: InvoiceType?,
    ) : InvoiceDetailAction

    data class OnIssueDate(
        val date: LocalDate?,
    ) : InvoiceDetailAction

    data class OnCorrective(
        val corrective: Boolean,
    ) : InvoiceDetailAction

    data class OnCause(
        val lineKey: Long,
        val cause: NoQuotaCause?,
    ) : InvoiceDetailAction

    data object OnAddLine : InvoiceDetailAction

    data class OnRemoveLine(
        val lineKey: Long,
    ) : InvoiceDetailAction

    data class OnUseTotal(
        val total: Money,
    ) : InvoiceDetailAction

    data object OnSave : InvoiceDetailAction

    data object OnConfirm : InvoiceDetailAction

    data object OnDiscard : InvoiceDetailAction

    data object OnDiscardDismiss : InvoiceDetailAction

    data object OnDiscardConfirm : InvoiceDetailAction

    data object OnRecognize : InvoiceDetailAction

    data object OnRecognizeDismiss : InvoiceDetailAction

    data object OnRecognizeConfirm : InvoiceDetailAction

    data object OnRetry : InvoiceDetailAction

    data object OnMessageShown : InvoiceDetailAction
}

/**
 * Review of one invoice. Every change travels with the version it was read at; a stale version
 * reloads the invoice instead of overwriting someone else's change (research D4). Confirming saves
 * pending edits first, so what gets confirmed is what is on screen (D5). While the server is still
 * recognising the document, the invoice is polled (D6).
 */
class InvoiceDetailViewModel(
    private val invoiceId: String,
    private val invoicing: InvoicingUseCases,
    private val pollIntervalMillis: Long = 5_000L,
    private val maxPolls: Int = 24,
) : ViewModel() {
    val form = InvoiceForm()
    private val lines = mutableMapOf<Long, LineForm>()
    private var nextKey = 0L
    private val _state = MutableStateFlow(InvoiceDetailState())
    val state = _state.asStateFlow()
    private var pollJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            val company = invoicing.repository.company()
            if (company is Result.Success) _state.update { it.copy(recognitionEnabled = company.data?.recognitionEnabled == true) }
        }
    }

    fun line(key: Long): LineForm = lines.getValue(key)

    /** What the form would send now, or null while a field does not parse. Reads Compose state. */
    fun currentDraft(): InvoiceDraft? = (parse() as? InvoiceForm.Parsed.Valid)?.draft

    val isDirty: Boolean get() {
        val invoice = _state.value.invoice ?: return false
        return currentDraft()?.let { it != invoice.draft() } ?: true
    }

    fun onAction(action: InvoiceDetailAction) {
        when (action) {
            is InvoiceDetailAction.OnType -> _state.update { it.copy(type = action.type) }
            is InvoiceDetailAction.OnIssueDate -> _state.update { it.copy(issueDate = action.date) }
            is InvoiceDetailAction.OnCorrective -> _state.update { it.copy(corrective = action.corrective) }
            is InvoiceDetailAction.OnCause -> _state.update { it.copy(causes = it.causes + (action.lineKey to action.cause)) }
            InvoiceDetailAction.OnAddLine -> {
                val key = newLine()
                _state.update { it.copy(lineKeys = it.lineKeys + key) }
            }
            is InvoiceDetailAction.OnRemoveLine -> {
                lines.remove(action.lineKey)
                _state.update { it.copy(lineKeys = it.lineKeys - action.lineKey, causes = it.causes - action.lineKey) }
            }
            is InvoiceDetailAction.OnUseTotal -> form.total.setTextAndPlaceCursorAtEnd(action.total.toInput())
            InvoiceDetailAction.OnSave -> launchBusy { save() }
            InvoiceDetailAction.OnConfirm -> launchBusy { confirm() }
            InvoiceDetailAction.OnDiscard -> _state.update { it.copy(confirmingDiscard = true) }
            InvoiceDetailAction.OnDiscardDismiss -> _state.update { it.copy(confirmingDiscard = false) }
            InvoiceDetailAction.OnDiscardConfirm -> {
                _state.update { it.copy(confirmingDiscard = false) }
                launchBusy { discard() }
            }
            InvoiceDetailAction.OnRecognize -> _state.update { it.copy(confirmingRecognize = true) }
            InvoiceDetailAction.OnRecognizeDismiss -> _state.update { it.copy(confirmingRecognize = false) }
            InvoiceDetailAction.OnRecognizeConfirm -> {
                _state.update { it.copy(confirmingRecognize = false) }
                launchBusy { recognize() }
            }
            InvoiceDetailAction.OnRetry -> load()
            InvoiceDetailAction.OnMessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            when (val result = invoicing.repository.invoice(invoiceId)) {
                is Result.Success -> {
                    show(result.data)
                    if (result.data.state == InvoiceState.PENDING_RECOGNITION) poll(sinceVersion = null)
                }
                is Result.Failure -> _state.update { it.copy(isLoading = false, loadError = result.error.toUiText()) }
            }
        }
    }

    /** Puts [invoice] on screen, replacing whatever the form held. */
    private fun show(invoice: Invoice) {
        lines.clear()
        val keys = invoice.lines.map { newLine() }
        form.fill(invoice, keys.map { lines.getValue(it) })
        _state.update {
            it.copy(
                invoice = invoice,
                isLoading = false,
                type = invoice.type,
                issueDate = invoice.issueDate,
                corrective = invoice.corrective,
                lineKeys = keys,
                causes = keys.zip(invoice.lines).associate { (key, line) -> key to line.noQuotaCause },
                badFields = emptySet(),
            )
        }
    }

    private fun newLine(): Long = nextKey++.also { lines[it] = LineForm(it) }

    private fun parse(): InvoiceForm.Parsed {
        val state = _state.value
        return form.parse(
            type = state.type,
            issueDate = state.issueDate,
            corrective = state.corrective,
            lines = state.lineKeys.mapNotNull { lines[it] },
            causes = state.causes,
            currency = state.invoice?.currency ?: "EUR",
            version = state.invoice?.version ?: 0,
        )
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (_state.value.isBusy || _state.value.invoice == null) return
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            try {
                block()
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    /** Saves the form; the saved invoice (with recalculated warnings) on success, null otherwise. */
    private suspend fun save(announce: Boolean = true): Invoice? {
        val draft =
            when (val parsed = parse()) {
                is InvoiceForm.Parsed.Invalid -> {
                    _state.update { it.copy(badFields = parsed.badFields, message = UiText.Resource(Res.string.fix_fields)) }
                    return null
                }
                is InvoiceForm.Parsed.Valid -> parsed.draft
            }
        return when (val result = invoicing.repository.save(invoiceId, draft)) {
            is Result.Success -> {
                show(result.data)
                if (announce) message(Res.string.saved)
                result.data
            }
            is Result.Failure -> {
                handle(result.error)
                null
            }
        }
    }

    private suspend fun confirm() {
        val current = _state.value.invoice ?: return
        val toConfirm = if (isDirty) save(announce = false) ?: return else current
        if (toConfirm.hasBlockingWarnings) {
            message(Res.string.fix_blocking)
            return
        }
        when (val result = invoicing.repository.confirm(invoiceId, toConfirm.version)) {
            is Result.Success -> {
                show(result.data)
                message(Res.string.confirmed)
            }
            is Result.Failure -> handle(result.error)
        }
    }

    private suspend fun discard() {
        val current = _state.value.invoice ?: return
        when (val result = invoicing.repository.discard(invoiceId, current.version)) {
            is Result.Success -> {
                show(result.data)
                message(Res.string.discarded)
            }
            is Result.Failure -> handle(result.error)
        }
    }

    private suspend fun recognize() {
        val version = _state.value.invoice?.version
        when (val result = invoicing.repository.recognize(invoiceId)) {
            is Result.Success -> {
                message(Res.string.recognizing)
                poll(sinceVersion = version)
            }
            is Result.Failure -> handle(result.error)
        }
    }

    /**
     * Polls until recognition is over: the invoice leaves pending, or its version moves past
     * [sinceVersion] (a re-run on a draft bumps it).
     */
    private fun poll(sinceVersion: Int?) {
        pollJob?.cancel()
        pollJob =
            viewModelScope.launch {
                repeat(maxPolls) {
                    delay(pollIntervalMillis)
                    val result = invoicing.repository.invoice(invoiceId)
                    if (result is Result.Success) {
                        val fresh = result.data
                        val done =
                            if (sinceVersion ==
                                null
                            ) {
                                fresh.state != InvoiceState.PENDING_RECOGNITION
                            } else {
                                fresh.version > sinceVersion
                            }
                        // Never overwrite edits in progress: their save meets the new version and reloads.
                        if (done) {
                            if (!isDirty) show(fresh)
                            return@launch
                        }
                    }
                }
            }
    }

    /** Errors that change what is true on the server reload the invoice so the screen matches it. */
    private suspend fun handle(error: InvoicingError) {
        when (error) {
            InvoicingError.StaleVersion -> {
                reload()
                message(Res.string.reloaded)
            }
            // The state moved on the server: show it. Coherence and duplicate errors keep the form,
            // so the person can fix what the message points at.
            InvoicingError.StateNotAllowed, InvoicingError.QuarterClosed -> {
                _state.update { it.copy(message = error.toUiText()) }
                reload()
            }
            else -> _state.update { it.copy(message = error.toUiText()) }
        }
    }

    private suspend fun reload() {
        val result = invoicing.repository.invoice(invoiceId)
        if (result is Result.Success) show(result.data)
    }

    private fun message(resource: org.jetbrains.compose.resources.StringResource) =
        _state.update { it.copy(message = UiText.Resource(resource)) }
}
