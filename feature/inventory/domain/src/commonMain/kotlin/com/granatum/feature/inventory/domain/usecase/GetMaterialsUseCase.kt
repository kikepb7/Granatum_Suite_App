package com.granatum.feature.inventory.domain.usecase

import com.granatum.feature.inventory.domain.model.MaterialFilter
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.repository.MaterialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetMaterialsUseCase(private val materialRepository: MaterialRepository) {
    operator fun invoke(filter: Flow<MaterialFilter>): Flow<List<MaterialModel>> =
        materialRepository.observeMaterials().combine(filter) { materials, currentFilter ->
            materials.filter(currentFilter::matches)
        }
}
