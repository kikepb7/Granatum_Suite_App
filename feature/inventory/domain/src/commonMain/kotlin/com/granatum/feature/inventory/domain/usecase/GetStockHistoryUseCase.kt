package com.granatum.feature.inventory.domain.usecase

import com.granatum.feature.inventory.domain.model.StockMovementModel
import com.granatum.feature.inventory.domain.repository.MaterialRepository
import kotlinx.coroutines.flow.Flow

class GetStockHistoryUseCase(private val materialRepository: MaterialRepository) {
    operator fun invoke(materialId: String): Flow<List<StockMovementModel>> =
        materialRepository.observeStockHistory(materialId = materialId)
}
