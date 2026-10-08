package com.granatum.feature.clockin.domain.repository

import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.model.CorrectionDraft
import com.granatum.feature.clockin.domain.model.CorrectionError
import com.granatum.feature.clockin.domain.model.CorrectionModel
import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.model.TodayState
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first: every punch lands on the device first and returns at once; the sync engine
 * registers it with the server later (specs/005-fichaje-real). Reads combine what the server
 * has recorded with what is still pending on the device.
 */
interface ClockInRepository {
    fun observeToday(): Flow<TodayState>
    fun observeMonth(year: Int, month: Int): Flow<List<ShiftModel>>
    fun observeShift(key: String): Flow<ShiftModel?>
    fun observePendingSyncCount(): Flow<Int>

    /** Downloads the month from the server; a no-op without coverage (FR-016). */
    suspend fun refreshMonth(year: Int, month: Int)

    suspend fun clockIn(): Result<Unit, ClockActionError>
    suspend fun clockOut(): Result<Unit, ClockActionError>
    suspend fun startBreak(type: BreakType): Result<Unit, ClockActionError>
    suspend fun endBreak(): Result<Unit, ClockActionError>

    suspend fun syncNow()

    suspend fun getCorrections(shiftServerId: String): Result<List<CorrectionModel>, CorrectionError>
    suspend fun requestCorrection(shiftServerId: String, draft: CorrectionDraft): Result<CorrectionModel, CorrectionError>
}
