package com.granatum.feature.inventory.presentation.category

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import com.granatum.feature.inventory.domain.validation.CategoryIssue
import com.granatum.feature.inventory.domain.validation.InventoryValidation
import com.granatum.feature.inventory.presentation.common.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryRow(val category: CategoryModel, val materialCount: Int) {
    /** The server cannot delete a category in use (it would answer 500): not offered (FR-013). */
    val canDelete: Boolean get() = materialCount == 0
}

data class CategoryListState(
    val rows: List<CategoryRow> = emptyList(),
    /** null: not editing; "" : creating; an id: editing that one. */
    val editingId: String? = null,
    val issues: Set<CategoryIssue> = emptySet(),
    val deleting: CategoryModel? = null,
    val error: UiText? = null,
    val isBusy: Boolean = false
)

sealed interface CategoryAction {
    data object OnNew : CategoryAction
    data class OnEdit(val category: CategoryModel) : CategoryAction
    data object OnDismissEditor : CategoryAction
    data object OnSave : CategoryAction
    data class OnDelete(val category: CategoryModel) : CategoryAction
    data object OnDismissDelete : CategoryAction
    data object OnConfirmDelete : CategoryAction
}

class CategoryListViewModel(private val inventory: InventoryUseCases) : ViewModel() {

    val name = TextFieldState()
    val description = TextFieldState()
    private val ui = MutableStateFlow(CategoryListState())

    val state = combine(inventory.repository.observeCategories(), inventory.repository.observeMaterials(), ui) { categories, materials, ui ->
        val counts = materials.groupingBy { it.category.id }.eachCount()
        ui.copy(rows = categories.map { CategoryRow(it, counts[it.id] ?: 0) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), CategoryListState())

    fun onAction(action: CategoryAction) {
        when (action) {
            CategoryAction.OnNew -> open("", "", "")
            is CategoryAction.OnEdit -> open(action.category.id, action.category.name, action.category.description.orEmpty())
            CategoryAction.OnDismissEditor -> ui.update { it.copy(editingId = null) }
            CategoryAction.OnSave -> save()
            is CategoryAction.OnDelete -> if (state.value.rows.firstOrNull { it.category.id == action.category.id }?.canDelete == true) {
                ui.update { it.copy(deleting = action.category) }
            }
            CategoryAction.OnDismissDelete -> ui.update { it.copy(deleting = null) }
            CategoryAction.OnConfirmDelete -> delete()
        }
    }

    private fun open(id: String, n: String, d: String) {
        name.setTextAndPlaceCursorAtEnd(n)
        description.setTextAndPlaceCursorAtEnd(d)
        ui.update { it.copy(editingId = id, issues = emptySet(), error = null) }
    }

    private fun save() {
        val id = ui.value.editingId ?: return
        if (ui.value.isBusy) return
        val n = name.text.toString()
        val d = description.text.toString().ifBlank { null }
        val issues = InventoryValidation.validateCategory(n, d)
        if (issues.isNotEmpty()) { ui.update { it.copy(issues = issues) }; return }
        ui.update { it.copy(isBusy = true, issues = emptySet(), error = null) }
        viewModelScope.launch {
            val result = if (id.isEmpty()) inventory.repository.createCategory(n, d) else inventory.repository.updateCategory(id, n, d)
            when (result) {
                is Result.Success -> ui.update { it.copy(isBusy = false, editingId = null) }
                is Result.Failure -> ui.update { it.copy(isBusy = false, error = result.error.toUiText()) }
            }
            // A rename shows up on every material of the category.
            if (result is Result.Success && id.isNotEmpty()) inventory.repository.refresh()
        }
    }

    private fun delete() {
        val category = ui.value.deleting ?: return
        ui.update { it.copy(isBusy = true, deleting = null) }
        viewModelScope.launch {
            val result = inventory.repository.deleteCategory(category.id)
            ui.update { it.copy(isBusy = false, error = (result as? Result.Failure)?.error?.toUiText()) }
        }
    }
}
