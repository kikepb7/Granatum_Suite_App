package com.granatum.feature.inventory.presentation.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.model.ProductFilter
import com.granatum.feature.inventory.domain.model.ProductModel
import com.granatum.feature.inventory.domain.model.StockFilter
import com.granatum.feature.inventory.domain.model.StorageLocation
import com.granatum.feature.inventory.domain.model.Warehouse
import com.granatum.feature.inventory.domain.model.filterBy
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The filters sheet edits a copy; the list only changes when it is applied. */
data class FiltersSheetState(val draft: ProductFilter, val matching: Int)

data class ProductListState(
    val products: List<ProductModel> = emptyList(),
    val categories: List<CategoryModel> = emptyList(),
    val filter: ProductFilter = ProductFilter(),
    val isEmptyInventory: Boolean = true,
    /** Units and out-of-stock products among those shown, so the summary follows the filters. */
    val totalUnits: Int = 0,
    val outOfStock: Int = 0,
    /** Products per section across the whole inventory, for the warehouse plan in the sheet. */
    val locationCounts: Map<StorageLocation, Int> = emptyMap(),
    val sheet: FiltersSheetState? = null,
    val isRefreshing: Boolean = false,
    /** The last refresh failed for lack of coverage: what is shown may be out of date. */
    val isStale: Boolean = false
)

/** The filters shown as removable pills under the search bar. */
enum class FilterPill { SECTION, CATEGORY, STOCK }

sealed interface ProductListAction {
    data class OnWarehouse(val warehouse: Warehouse?) : ProductListAction
    data class OnRemove(val pill: FilterPill) : ProductListAction
    data object OnClearFilters : ProductListAction
    data object OnOpenFilters : ProductListAction
    data class OnDraftChange(val draft: ProductFilter) : ProductListAction
    data object OnResetDraft : ProductListAction
    data object OnApplyFilters : ProductListAction
    data object OnDismissFilters : ProductListAction
    data object OnRefresh : ProductListAction
}

private data class ListUi(
    val filter: ProductFilter = ProductFilter(),
    val draft: ProductFilter? = null,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false
)

class ProductListViewModel(private val inventory: InventoryUseCases) : ViewModel() {

    val query = TextFieldState()
    private val ui = MutableStateFlow(ListUi())

    val state = combine(
        inventory.repository.observeProducts(),
        inventory.repository.observeCategories(),
        snapshotFlow { query.text.toString() },
        ui
    ) { products, categories, text, ui ->
        val filter = ui.filter.copy(query = text)
        val shown = products.filterBy(filter)
        ProductListState(
            products = shown,
            categories = categories,
            filter = filter,
            isEmptyInventory = products.isEmpty(),
            totalUnits = shown.sumOf { it.quantity },
            outOfStock = shown.count { it.isOutOfStock },
            locationCounts = products.mapNotNull { it.location }.groupingBy { it }.eachCount(),
            sheet = ui.draft?.let { draft -> FiltersSheetState(draft, products.filterBy(draft.copy(query = text)).size) },
            isRefreshing = ui.isRefreshing,
            isStale = ui.isStale
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ProductListState())

    init {
        refresh()
    }

    fun onAction(action: ProductListAction) {
        when (action) {
            is ProductListAction.OnWarehouse -> updateFilter { it.copy(warehouse = action.warehouse) }
            is ProductListAction.OnRemove -> updateFilter {
                when (action.pill) {
                    FilterPill.SECTION -> it.copy(section = null)
                    FilterPill.CATEGORY -> it.copy(categoryId = null)
                    FilterPill.STOCK -> it.copy(stock = StockFilter.ALL)
                }
            }
            ProductListAction.OnClearFilters -> updateFilter { it.cleared() }
            ProductListAction.OnOpenFilters -> ui.update { it.copy(draft = it.filter) }
            is ProductListAction.OnDraftChange -> ui.update { if (it.draft == null) it else it.copy(draft = action.draft) }
            ProductListAction.OnResetDraft -> ui.update { it.copy(draft = it.draft?.cleared()?.copy(sort = ProductFilter().sort)) }
            ProductListAction.OnApplyFilters -> ui.update { it.copy(filter = it.draft ?: it.filter, draft = null) }
            ProductListAction.OnDismissFilters -> ui.update { it.copy(draft = null) }
            ProductListAction.OnRefresh -> refresh()
        }
    }

    private fun updateFilter(change: (ProductFilter) -> ProductFilter) = ui.update { it.copy(filter = change(it.filter)) }

    private fun refresh() {
        if (ui.value.isRefreshing) return
        viewModelScope.launch {
            ui.update { it.copy(isRefreshing = true) }
            val result = inventory.repository.refresh()
            ui.update { it.copy(isRefreshing = false, isStale = (result as? Result.Failure)?.error == InventoryError.NoInternet) }
        }
    }
}
