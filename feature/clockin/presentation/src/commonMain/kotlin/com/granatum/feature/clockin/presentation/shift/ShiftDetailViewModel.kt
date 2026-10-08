package com.granatum.feature.clockin.presentation.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.domain.model.CorrectionModel
import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.usecase.GetCorrectionsUseCase
import com.granatum.feature.clockin.domain.usecase.ObserveShiftUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShiftDetailUiState(
    val shift: ShiftModel? = null,
    val loaded: Boolean = false,
    val corrections: List<CorrectionModel> = emptyList(),
    val correctionsUnavailable: Boolean = false
)

class ShiftDetailViewModel(
    shiftKey: String,
    observeShift: ObserveShiftUseCase,
    private val getCorrections: GetCorrectionsUseCase
) : ViewModel() {

    private val corrections = MutableStateFlow<Pair<List<CorrectionModel>, Boolean>>(emptyList<CorrectionModel>() to false)

    val state = combine(observeShift(shiftKey), corrections) { shift, (list, unavailable) ->
        ShiftDetailUiState(shift = shift, loaded = true, corrections = list, correctionsUnavailable = unavailable)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ShiftDetailUiState())

    init {
        viewModelScope.launch {
            val serverId = observeShift(shiftKey).filterNotNull().first().serverId ?: return@launch
            reload(serverId)
        }
    }

    /** Corrections are only on the server; without coverage the screen says so (FR-022). */
    fun reload(serverId: String) {
        viewModelScope.launch {
            when (val result = getCorrections(serverId)) {
                is Result.Success -> corrections.update { result.data to false }
                is Result.Failure -> corrections.update { it.first to true }
            }
        }
    }
}
