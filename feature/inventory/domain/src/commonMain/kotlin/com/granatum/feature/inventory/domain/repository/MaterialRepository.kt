package com.granatum.feature.inventory.domain.repository

import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.domain.model.MaterialDraft
import com.granatum.feature.inventory.domain.model.MaterialModel
import com.granatum.feature.inventory.domain.model.StockMovementModel
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first facade: reads always come from the local Room cache so the
 * list/detail screens work in the warehouse regardless of connectivity.
 * Writes (create/edit/quantity update) require network — there is no local
 * write queue here, unlike `ClockInRepository`. See ARCHITECTURE.md for why
 * that tradeoff is fine for inventory but not for clock punches.
 */
interface MaterialRepository {
    fun observeMaterials(): Flow<List<MaterialModel>>
    fun observeMaterial(materialId: String): Flow<MaterialModel?>
    fun observeStockHistory(materialId: String): Flow<List<StockMovementModel>>
    suspend fun refreshMaterials(): EmptyResult<DataError.Remote>
    suspend fun saveMaterial(draft: MaterialDraft): Result<MaterialModel, DataError.Remote>
    suspend fun updateQuantity(
        materialId: String,
        newQuantity: Int,
        reason: String
    ): Result<MaterialModel, DataError.Remote>
}
