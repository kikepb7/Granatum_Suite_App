package com.granatum.feature.inventory.domain.usecase

import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.feature.inventory.domain.repository.MaterialRepository

class RefreshMaterialsUseCase(private val materialRepository: MaterialRepository) {
    suspend operator fun invoke(): EmptyResult<DataError.Remote> = materialRepository.refreshMaterials()
}
