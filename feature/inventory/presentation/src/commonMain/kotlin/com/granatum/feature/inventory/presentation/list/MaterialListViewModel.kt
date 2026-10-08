package com.granatum.feature.inventory.presentation.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.MaterialCondition
import com.granatum.feature.inventory.domain.model.MaterialFilter
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.filterBy
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MaterialListState(
    val materials: List<MaterialModel> = emptyList(),
    val categories: List<CategoryModel> = emptyList(),
    val filter: MaterialFilter = MaterialFilter(),
    val isEmptyInventory: Boolean = true,
    val isRefreshing: Boolean = false,
    /** The last refresh failed for lack of coverage: what is shown may be out of date (FR-003). */
    val isStale: Boolean = false
)

sealed interface MaterialListAction {
    data class OnCategory(val categoryId: String?) : MaterialListAction
    data class OnCondition(val condition: MaterialCondition?) : MaterialListAction
    data object OnToggleOutOfStock : MaterialListAction
    data object OnRefresh : MaterialListAction
}

class MaterialListViewModel(private val inventory: InventoryUseCases) : ViewModel() {

    val query = TextFieldState()
    private val ui = MutableStateFlow(MaterialListState())

    val state = combine(
        inventory.repository.observeMaterials(),
        inventory.repository.observeCategories(),
        snapshotFlow { query.text.toString() },
        ui
    ) { materials, categories, text, ui ->
        val filter = ui.filter.copy(query = text)
        ui.copy(materials = materials.filterBy(filter), categories = categories, filter = filter, isEmptyInventory = materials.isEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), MaterialListState())

    init {
        refresh()
    }

    fun onAction(action: MaterialListAction) {
        when (action) {
            is MaterialListAction.OnCategory -> ui.update { it.copy(filter = it.filter.copy(categoryId = action.categoryId)) }
            is MaterialListAction.OnCondition -> ui.update { it.copy(filter = it.filter.copy(condition = action.condition)) }
            MaterialListAction.OnToggleOutOfStock -> ui.update { it.copy(filter = it.filter.copy(onlyOutOfStock = !it.filter.onlyOutOfStock)) }
            MaterialListAction.OnRefresh -> refresh()
        }
    }

    private fun refresh() {
        if (ui.value.isRefreshing) return
        viewModelScope.launch {
            ui.update { it.copy(isRefreshing = true) }
            val result = inventory.repository.refresh()
            ui.update { it.copy(isRefreshing = false, isStale = (result as? Result.Failure)?.error == InventoryError.NoInternet) }
        }
    }
}
