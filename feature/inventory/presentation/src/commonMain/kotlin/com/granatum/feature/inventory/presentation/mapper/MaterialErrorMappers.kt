package com.granatum.feature.inventory.presentation.mapper

import com.granatum.core.presentation.mapper.toUiText
import com.granatum.core.presentation.util.UiText
import com.granatum.core.presentation.util.UiText.DynamicString
import com.granatum.feature.inventory.domain.model.SaveMaterialError
import com.granatum.feature.inventory.domain.model.UpdateQuantityError

fun SaveMaterialError.toUiText(): UiText = when (this) {
    SaveMaterialError.BlankName -> DynamicString(value = "El nombre no puede estar vacío")
    SaveMaterialError.NegativeQuantity -> DynamicString(value = "La cantidad no puede ser negativa")
    is SaveMaterialError.Remote -> dataError.toUiText()
}

fun UpdateQuantityError.toUiText(): UiText = when (this) {
    UpdateQuantityError.BlankReason -> DynamicString(value = "Indica un motivo para el cambio de cantidad")
    UpdateQuantityError.NegativeQuantity -> DynamicString(value = "La cantidad no puede ser negativa")
    UpdateQuantityError.NoChange -> DynamicString(value = "La nueva cantidad es igual a la actual")
    is UpdateQuantityError.Remote -> dataError.toUiText()
}
