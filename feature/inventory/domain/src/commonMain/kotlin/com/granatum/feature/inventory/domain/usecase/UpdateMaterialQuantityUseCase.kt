package com.granatum.feature.inventory.domain.usecase

import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.Result.Failure
import com.granatum.core.domain.util.Result.Success
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.UpdateQuantityError
import com.granatum.feature.inventory.domain.model.UpdateQuantityError.BlankReason
import com.granatum.feature.inventory.domain.model.UpdateQuantityError.NegativeQuantity
import com.granatum.feature.inventory.domain.model.UpdateQuantityError.NoChange
import com.granatum.feature.inventory.domain.model.UpdateQuantityError.Remote
import com.granatum.feature.inventory.domain.repository.MaterialRepository

/**
 * Enforces the product rule that a stock quantity can never change without a
 * reason ("actualización rápida de cantidad, con motivo obligatorio").
 */
class UpdateMaterialQuantityUseCase(private val materialRepository: MaterialRepository) {

    suspend operator fun invoke(
        materialId: String,
        currentQuantity: Int,
        newQuantity: Int,
        reason: String
    ): Result<MaterialModel, UpdateQuantityError> {
        if (newQuantity < 0) return Failure(error = NegativeQuantity)
        if (newQuantity == currentQuantity) return Failure(error = NoChange)
        if (reason.isBlank()) return Failure(error = BlankReason)

        return when (
            val result = materialRepository.updateQuantity(
                materialId = materialId,
                newQuantity = newQuantity,
                reason = reason.trim()
            )
        ) {
            is Success -> result
            is Failure -> Failure(error = Remote(dataError = result.error))
        }
    }
}
