package com.granatum.feature.inventory.domain.model

import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Error

sealed interface SaveMaterialError : Error {
    data object BlankName : SaveMaterialError
    data object NegativeQuantity : SaveMaterialError
    data class Remote(val dataError: DataError.Remote) : SaveMaterialError
}

sealed interface UpdateQuantityError : Error {
    data object BlankReason : UpdateQuantityError
    data object NegativeQuantity : UpdateQuantityError
    data object NoChange : UpdateQuantityError
    data class Remote(val dataError: DataError.Remote) : UpdateQuantityError
}
