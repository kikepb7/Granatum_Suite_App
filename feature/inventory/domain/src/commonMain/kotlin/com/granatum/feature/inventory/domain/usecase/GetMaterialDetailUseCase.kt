package com.granatum.feature.inventory.domain.usecase

import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.repository.MaterialRepository
import kotlinx.coroutines.flow.Flow

class GetMaterialDetailUseCase(private val materialRepository: MaterialRepository) {
    operator fun invoke(materialId: String): Flow<MaterialModel?> =
        materialRepository.observeMaterial(materialId = materialId)
}
