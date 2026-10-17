package com.granatum.feature.events.presentation.detail

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.domain.model.AvailableMaterial
import com.granatum.feature.events.domain.model.Event
import com.granatum.feature.events.domain.model.EventsError
import com.granatum.feature.events.domain.provider.TodayProvider
import com.granatum.feature.events.domain.usecase.DeleteEventUseCase
import com.granatum.feature.events.domain.usecase.FetchAvailableMaterialsUseCase
import com.granatum.feature.events.domain.usecase.FinishEventUseCase
import com.granatum.feature.events.domain.usecase.ObserveEventUseCase
import com.granatum.feature.events.domain.usecase.RefreshEventsUseCase
import com.granatum.feature.events.domain.usecase.SetEventMaterialUseCase
import com.granatum.feature.events.presentation.common.EventDateFormatter
import com.granatum.feature.events.presentation.common.EventUiMapper
import com.granatum.feature.events.presentation.common.toUiText
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.event_finished_message
import granatumsuite.feature.events.presentation.generated.resources.material_added
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** The typed values behind the screen; [EventDetailUiState] is derived from them and the event. */
private data class EventDetailInternalState(
    val refreshed: Boolean = false,
    /** Quantities chosen with − / + that have not reached the server yet, by material. */
    val pendingQuantities: Map<String, Int> = emptyMap(),
    val pickerOpen: Boolean = false,
    val available: List<AvailableMaterial>? = null,
    val pickerError: UiText? = null,
    val selectedMaterialId: String? = null,
    val selectedQuantity: Int = 1,
    val pickerSaving: Boolean = false,
    val dialog: EventDetailDialog? = null,
    val isBusy: Boolean = false
)

internal class EventDetailViewModel(
    private val eventId: String,
    private val canManage: Boolean,
    observeEventUseCase: ObserveEventUseCase,
    private val refreshEventsUseCase: RefreshEventsUseCase,
    private val setEventMaterialUseCase: SetEventMaterialUseCase,
    private val finishEventUseCase: FinishEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val fetchAvailableMaterialsUseCase: FetchAvailableMaterialsUseCase,
    private val mapper: EventUiMapper,
    private val dates: EventDateFormatter,
    private val todayProvider: TodayProvider
) : ViewModel() {

    /** The picker's search text. */
    val pickerQuery = TextFieldState()

    private val internalState = MutableStateFlow(EventDetailInternalState())
    private val _uiEvents = MutableSharedFlow<EventDetailUiEvent>()
    val uiEvents: SharedFlow<EventDetailUiEvent> = _uiEvents.asSharedFlow()
    private val quantityJobs = mutableMapOf<String, Job>()

    val uiState: StateFlow<EventDetailScreenUiState> =
        combine(observeEventUseCase.observe(eventId), internalState, snapshotFlow { pickerQuery.text.toString() }) { event, internal, query ->
            when {
                event != null -> EventDetailScreenUiState.Loaded(event.toUiState(internal, query))
                internal.refreshed -> EventDetailScreenUiState.NotFound
                else -> EventDetailScreenUiState.Loading
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDetailScreenUiState.Loading)

    init {
        refresh()
    }

    fun onAction(action: EventDetailUiAction) {
        when (action) {
            EventDetailUiAction.Refresh -> refresh()
            is EventDetailUiAction.ChangeQuantity -> changeQuantity(action.materialId, action.quantity)
            is EventDetailUiAction.RemoveMaterial -> changeQuantity(action.materialId, 0, immediately = true)
            EventDetailUiAction.OpenPicker -> openPicker()
            EventDetailUiAction.ClosePicker -> closePicker()
            EventDetailUiAction.RetryPicker -> loadAvailable()
            is EventDetailUiAction.SelectPickerMaterial -> internalState.update {
                if (it.available?.firstOrNull { m -> m.id == action.materialId }?.quantity?.let { free -> free > 0 } == true) {
                    it.copy(selectedMaterialId = action.materialId, selectedQuantity = 1)
                } else {
                    it
                }
            }
            is EventDetailUiAction.ChangePickerQuantity -> internalState.update { state ->
                val max = state.available?.firstOrNull { it.id == state.selectedMaterialId }?.quantity ?: 1
                state.copy(selectedQuantity = action.quantity.coerceIn(1, max.coerceAtLeast(1)))
            }
            EventDetailUiAction.ConfirmPicker -> confirmPicker()
            EventDetailUiAction.RequestFinish -> internalState.update { it.copy(dialog = EventDetailDialog.FINISH) }
            EventDetailUiAction.RequestDelete -> internalState.update { it.copy(dialog = EventDetailDialog.DELETE) }
            EventDetailUiAction.DismissDialog -> internalState.update { it.copy(dialog = null) }
            EventDetailUiAction.ConfirmDialog -> when (internalState.value.dialog) {
                EventDetailDialog.FINISH -> finish()
                EventDetailDialog.DELETE -> delete()
                null -> Unit
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            refreshEventsUseCase.refresh(eventId)
            internalState.update { it.copy(refreshed = true) }
        }
    }

    /**
     * − and + change the number shown at once; the server hears the final figure a moment after
     * the last tap, so five taps are one request (research D3). A failure puts the stored figure
     * back and says why - for lack of stock, how many units are left.
     */
    private fun changeQuantity(materialId: String, quantity: Int, immediately: Boolean = false) {
        val target = quantity.coerceAtLeast(0)
        internalState.update { it.copy(pendingQuantities = it.pendingQuantities + (materialId to target)) }
        quantityJobs.remove(materialId)?.cancel()
        quantityJobs[materialId] = viewModelScope.launch {
            if (!immediately) delay(QUANTITY_DEBOUNCE_MILLIS)
            val result = setEventMaterialUseCase.set(eventId, materialId, target)
            internalState.update { it.copy(pendingQuantities = it.pendingQuantities - materialId) }
            if (result is Result.Failure) _uiEvents.emit(EventDetailUiEvent.ShowMessage(result.error.toUiText()))
        }
    }

    private fun openPicker() {
        pickerQuery.clearText()
        internalState.update { it.copy(pickerOpen = true, selectedMaterialId = null, selectedQuantity = 1) }
        loadAvailable()
    }

    private fun closePicker() {
        internalState.update { it.copy(pickerOpen = false, selectedMaterialId = null, available = null, pickerError = null) }
    }

    /** The stock changes all the time: it is fetched every time the sheet opens. */
    private fun loadAvailable() {
        internalState.update { it.copy(available = null, pickerError = null) }
        viewModelScope.launch {
            when (val result = fetchAvailableMaterialsUseCase.fetch()) {
                is Result.Success -> internalState.update { it.copy(available = result.data) }
                is Result.Failure -> internalState.update { it.copy(pickerError = result.error.toUiText(), available = emptyList()) }
            }
        }
    }

    private fun confirmPicker() {
        val state = internalState.value
        val materialId = state.selectedMaterialId ?: return
        if (state.pickerSaving) return
        val alreadyInEvent = (uiState.value as? EventDetailScreenUiState.Loaded)?.data?.materials
            ?.firstOrNull { it.materialId == materialId }?.quantity ?: 0
        internalState.update { it.copy(pickerSaving = true) }
        viewModelScope.launch {
            when (val result = setEventMaterialUseCase.set(eventId, materialId, alreadyInEvent + state.selectedQuantity)) {
                is Result.Success -> {
                    internalState.update { it.copy(pickerSaving = false) }
                    closePicker()
                    _uiEvents.emit(EventDetailUiEvent.ShowMessage(UiText.Resource(Res.string.material_added)))
                }
                is Result.Failure -> {
                    internalState.update { it.copy(pickerSaving = false, selectedMaterialId = null) }
                    _uiEvents.emit(EventDetailUiEvent.ShowMessage(result.error.toUiText()))
                    // Someone took units meanwhile: show what is really left.
                    if (result.error is EventsError.InsufficientStock) loadAvailable()
                }
            }
        }
    }

    private fun finish() {
        internalState.update { it.copy(dialog = null, isBusy = true) }
        viewModelScope.launch {
            val message = when (val result = finishEventUseCase.finish(eventId)) {
                is Result.Success -> UiText.Resource(Res.string.event_finished_message)
                is Result.Failure -> result.error.toUiText()
            }
            internalState.update { it.copy(isBusy = false) }
            _uiEvents.emit(EventDetailUiEvent.ShowMessage(message))
        }
    }

    private fun delete() {
        internalState.update { it.copy(dialog = null, isBusy = true) }
        viewModelScope.launch {
            when (val result = deleteEventUseCase.delete(eventId)) {
                is Result.Success -> _uiEvents.emit(EventDetailUiEvent.Deleted)
                is Result.Failure -> _uiEvents.emit(EventDetailUiEvent.ShowMessage(result.error.toUiText()))
            }
            internalState.update { it.copy(isBusy = false) }
        }
    }

    private fun Event.toUiState(internal: EventDetailInternalState, query: String): EventDetailUiState {
        val today = todayProvider.today()
        val shownMaterials = materials.mapNotNull { material ->
            val pending = internal.pendingQuantities[material.materialId]
            if (pending == 0) return@mapNotNull null
            mapper.toMaterial(material, quantity = pending ?: material.quantity, isSaving = pending != null)
        }
        return EventDetailUiState(
            id = id,
            name = name,
            description = description,
            dayLabel = dates.day(date),
            monthLabel = dates.monthBadge(date),
            dateLabel = dates.long(date),
            relativeLabel = dates.relative(date, today),
            isFinished = isFinished,
            finishedOnLabel = finishedAt?.let { dates.short(it.toLocalDateTime(TimeZone.currentSystemDefault()).date) },
            materials = shownMaterials,
            materialsLabel = mapper.materialsLabel(this),
            canEdit = canManage && !isFinished,
            picker = if (internal.pickerOpen && canManage && !isFinished) picker(internal, query) else null,
            dialog = internal.dialog,
            isBusy = internal.isBusy
        )
    }

    private fun Event.picker(internal: EventDetailInternalState, query: String): MaterialPickerUiState {
        val inEvent = materials.associate { it.materialId to it.quantity }
        val needle = query.trim().lowercase()
        val items = internal.available.orEmpty()
            .filter { needle.isEmpty() || needle in it.category.lowercase() || needle in it.description.lowercase() }
            .map {
                PickerItemUiState(
                    id = it.id,
                    category = it.category,
                    description = it.description,
                    freeUnits = it.quantity,
                    inEvent = inEvent[it.id] ?: 0,
                    locationLabel = mapper.locationLabel(it.location),
                    imageUrl = it.imageUrl
                )
            }
        val selected = internal.available?.firstOrNull { it.id == internal.selectedMaterialId }
        return MaterialPickerUiState(
            isLoading = internal.available == null,
            error = internal.pickerError,
            items = items,
            selection = selected?.let {
                PickerSelectionUiState(
                    materialId = it.id,
                    label = "${it.category} · ${it.description}",
                    quantity = internal.selectedQuantity,
                    max = it.quantity
                )
            },
            isSaving = internal.pickerSaving
        )
    }

    private companion object {
        const val QUANTITY_DEBOUNCE_MILLIS = 600L
    }
}
