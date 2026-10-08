package com.granatum.feature.inventory.presentation.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.granatum.core.domain.util.Result
import com.granatum.core.presentation.util.UiText
import com.granatum.feature.inventory.domain.model.CategoryModel
import com.granatum.feature.inventory.domain.model.MaterialCondition
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.MaterialSize
import com.granatum.feature.inventory.domain.model.SizeUnit
import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import com.granatum.feature.inventory.domain.validation.DraftIssue
import com.granatum.feature.inventory.domain.validation.InventoryValidation
import com.granatum.feature.inventory.presentation.common.measureLabel
import com.granatum.feature.inventory.presentation.common.toDecimalOrNull
import com.granatum.feature.inventory.presentation.common.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Numeric fields that did not parse; reported separately from the domain's rules. */
enum class NumberField { AVAILABLE, TOTAL, HEIGHT, WIDTH, DIAMETER, PRICE }

data class MaterialFormState(
    val isEdit: Boolean = false,
    val ready: Boolean = false,
    val categories: List<CategoryModel> = emptyList(),
    val categoryId: String? = null,
    val condition: MaterialCondition = MaterialCondition.NUEVO,
    val unit: SizeUnit = SizeUnit.CM,
    val photos: List<String> = emptyList(),
    val issues: Set<DraftIssue> = emptySet(),
    val badNumbers: Set<NumberField> = emptySet(),
    val error: UiText? = null,
    val isSaving: Boolean = false
)

sealed interface MaterialFormAction {
    data class OnCategory(val id: String) : MaterialFormAction
    data class OnCondition(val condition: MaterialCondition) : MaterialFormAction
    data class OnUnit(val unit: SizeUnit) : MaterialFormAction
    data object OnSave : MaterialFormAction
}

sealed interface MaterialFormEvent {
    data class Saved(val id: String) : MaterialFormEvent
}

class MaterialFormViewModel(
    private val materialId: String?,
    private val inventory: InventoryUseCases
) : ViewModel() {

    val name = TextFieldState()
    val available = TextFieldState("0")
    val total = TextFieldState("0")
    val height = TextFieldState()
    val width = TextFieldState()
    val diameter = TextFieldState()
    val color = TextFieldState()
    val physicalMaterial = TextFieldState()
    val location = TextFieldState()
    val price = TextFieldState()
    val supplier = TextFieldState()

    private val ui = MutableStateFlow(MaterialFormState(isEdit = materialId != null, ready = materialId == null))
    private val events = Channel<MaterialFormEvent>()
    val eventFlow = events.receiveAsFlow()

    val state = combine(inventory.repository.observeCategories(), ui) { categories, ui -> ui.copy(categories = categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ui.value)

    init {
        if (materialId != null) {
            viewModelScope.launch { prefill(inventory.repository.observeMaterial(materialId).filterNotNull().first()) }
        }
    }

    private fun prefill(m: MaterialModel) {
        name.setTextAndPlaceCursorAtEnd(m.name)
        available.setTextAndPlaceCursorAtEnd(m.available.toString())
        total.setTextAndPlaceCursorAtEnd(m.total.toString())
        height.setTextAndPlaceCursorAtEnd(m.size.height.measureLabel())
        width.setTextAndPlaceCursorAtEnd(m.size.width.measureLabel())
        diameter.setTextAndPlaceCursorAtEnd(m.size.diameter?.measureLabel().orEmpty())
        color.setTextAndPlaceCursorAtEnd(m.color)
        physicalMaterial.setTextAndPlaceCursorAtEnd(m.physicalMaterial)
        location.setTextAndPlaceCursorAtEnd(m.location)
        price.setTextAndPlaceCursorAtEnd(m.unitPrice.measureLabel())
        supplier.setTextAndPlaceCursorAtEnd(m.supplier)
        ui.update { it.copy(categoryId = m.category.id, condition = m.condition, unit = m.size.unit, photos = m.photos, ready = true) }
    }

    fun onAction(action: MaterialFormAction) {
        when (action) {
            is MaterialFormAction.OnCategory -> ui.update { it.copy(categoryId = action.id, issues = it.issues - DraftIssue.CATEGORY_REQUIRED) }
            is MaterialFormAction.OnCondition -> ui.update { it.copy(condition = action.condition) }
            is MaterialFormAction.OnUnit -> ui.update { it.copy(unit = action.unit) }
            MaterialFormAction.OnSave -> save()
        }
    }

    private fun save() {
        if (ui.value.isSaving) return
        val bad = mutableSetOf<NumberField>()
        fun int(f: NumberField, s: TextFieldState) = s.text.toString().trim().toIntOrNull() ?: run { bad += f; 0 }
        fun dec(f: NumberField, s: TextFieldState) = s.text.toString().toDecimalOrNull() ?: run { bad += f; 0.0 }
        val diameterText = diameter.text.toString().trim()
        val draft = MaterialDraft(
            name = name.text.toString(),
            categoryId = ui.value.categoryId,
            available = int(NumberField.AVAILABLE, available),
            total = int(NumberField.TOTAL, total),
            size = MaterialSize(
                height = dec(NumberField.HEIGHT, height),
                width = dec(NumberField.WIDTH, width),
                diameter = if (diameterText.isEmpty()) null else dec(NumberField.DIAMETER, diameter),
                unit = ui.value.unit
            ),
            color = color.text.toString(),
            physicalMaterial = physicalMaterial.text.toString(),
            condition = ui.value.condition,
            location = location.text.toString(),
            unitPrice = dec(NumberField.PRICE, price),
            supplier = supplier.text.toString(),
            photos = ui.value.photos
        )
        val issues = InventoryValidation.validate(draft)
        if (bad.isNotEmpty() || issues.isNotEmpty()) {
            ui.update { it.copy(issues = issues, badNumbers = bad, error = null) }
            return
        }
        ui.update { it.copy(isSaving = true, issues = emptySet(), badNumbers = emptySet(), error = null) }
        viewModelScope.launch {
            val result = if (materialId == null) inventory.repository.create(draft) else inventory.repository.update(materialId, draft)
            when (result) {
                is Result.Success -> events.send(MaterialFormEvent.Saved(result.data.id))
                is Result.Failure -> ui.update { it.copy(error = result.error.toUiText()) }
            }
            ui.update { it.copy(isSaving = false) }
        }
    }
}
