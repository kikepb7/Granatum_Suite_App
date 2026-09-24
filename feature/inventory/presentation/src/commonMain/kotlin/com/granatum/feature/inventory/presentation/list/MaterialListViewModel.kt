package com.granatum.feature.inventory.presentation.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result.Failure
import com.granatum.core.domain.util.Result.Success
import com.granatum.core.presentation.mapper.toUiText
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.MaterialCategory
import com.granatum.feature.inventory.domain.model.MaterialFilter
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.MaterialStatus
import com.granatum.feature.inventory.domain.usecase.GetMaterialsUseCase
import com.granatum.feature.inventory.domain.usecase.RefreshMaterialsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MaterialListViewModel(
    private val getMaterialsUseCase: GetMaterialsUseCase,
    private val refreshMaterialsUseCase: RefreshMaterialsUseCase
) : ViewModel() {

    private var hasLoadedInitialData = false

    val queryState = TextFieldState()

    private val filter = MutableStateFlow(MaterialFilter())

    private val eventChannel = Channel<MaterialListEvent>()
    val events = eventChannel.receiveAsFlow()

    private val materials = getMaterialsUseCase(filter = filter)

    private val _uiState = MutableStateFlow(MaterialListUiState())
    val state = combine(_uiState, materials, filter) { uiState, items, currentFilter ->
        uiState.copy(items = items, filter = currentFilter)
    }
        .onStart {
            if (!hasLoadedInitialData) {
                observeQueryChanges()
                refresh()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = MaterialListUiState()
        )

    fun onAction(action: MaterialListAction) {
        when (action) {
            MaterialListAction.OnRefresh -> refresh()
            is MaterialListAction.OnCategorySelected -> filter.update { it.copy(category = action.category) }
            is MaterialListAction.OnStatusSelected -> filter.update { it.copy(status = action.status) }
            is MaterialListAction.OnMaterialClick -> sendEvent(MaterialListEvent.NavigateToDetail(action.materialId))
            MaterialListAction.OnAddClick -> sendEvent(MaterialListEvent.NavigateToCreate)
        }
    }

    private fun observeQueryChanges() {
        snapshotFlow { queryState.text.toString() }
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { query -> filter.update { it.copy(query = query) } }
            .launchIn(scope = viewModelScope)
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val result = refreshMaterialsUseCase()) {
                is Success -> Unit
                is Failure -> eventChannel.send(MaterialListEvent.Error(result.error.toUiText()))
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun sendEvent(event: MaterialListEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}

data class MaterialListUiState(
    val items: List<MaterialModel> = emptyList(),
    val filter: MaterialFilter = MaterialFilter(),
    val isRefreshing: Boolean = false
)

sealed interface MaterialListAction {
    data object OnRefresh : MaterialListAction
    data object OnAddClick : MaterialListAction
    data class OnCategorySelected(val category: MaterialCategory?) : MaterialListAction
    data class OnStatusSelected(val status: MaterialStatus?) : MaterialListAction
    data class OnMaterialClick(val materialId: String) : MaterialListAction
}

sealed interface MaterialListEvent {
    data class NavigateToDetail(val materialId: String) : MaterialListEvent
    data object NavigateToCreate : MaterialListEvent
    data class Error(val message: UiText) : MaterialListEvent
}
