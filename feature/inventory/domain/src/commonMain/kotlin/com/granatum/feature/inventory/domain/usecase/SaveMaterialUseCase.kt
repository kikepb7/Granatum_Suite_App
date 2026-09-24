package com.granatum.feature.inventory.domain.usecase

import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.Result.Failure
import com.granatum.core.domain.util.Result.Success
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.SaveMaterialError
import com.granatum.feature.inventory.domain.model.SaveMaterialError.BlankName
import com.granatum.feature.inventory.domain.model.SaveMaterialError.NegativeQuantity
import com.granatum.feature.inventory.domain.model.SaveMaterialError.Remote
import com.granatum.feature.inventory.domain.repository.MaterialRepository

class SaveMaterialUseCase(private val materialRepository: MaterialRepository) {

    suspend operator fun invoke(draft: MaterialDraft): Result<MaterialModel, SaveMaterialError> {
        if (draft.name.isBlank()) return Failure(error = BlankName)
        if (draft.quantity < 0) return Failure(error = NegativeQuantity)

        return when (val result = materialRepository.saveMaterial(draft = draft)) {
            is Success -> result
            is Failure -> Failure(error = Remote(dataError = result.error))
        }
    }
}
