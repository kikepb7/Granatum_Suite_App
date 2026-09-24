package com.granatum.feature.clockin.domain.repository

import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.model.ClockEventModel
import com.granatum.feature.clockin.domain.model.DailyAttendanceSummary
import com.granatum.feature.clockin.domain.model.RequestCorrectionError
import com.granatum.feature.clockin.domain.model.ShiftStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Offline-first by design, not just by cache: every write lands in Room
 * first and returns immediately, so the "Fichar entrada/salida" button never
 * blocks on the warehouse's flaky wifi. `ClockEventSyncManager` (data module)
 * drains the queue in the background. See ARCHITECTURE.md for the full flow.
 */
interface ClockInRepository {
    fun observeCurrentStatus(): Flow<ShiftStatus>
    fun observeTodayEvents(): Flow<List<ClockEventModel>>
    fun observeHistory(from: LocalDate, to: LocalDate): Flow<List<DailyAttendanceSummary>>
    fun observePendingSyncCount(): Flow<Int>

    suspend fun clockIn(): Result<Unit, ClockActionError>
    suspend fun clockOut(): Result<Unit, ClockActionError>
    suspend fun startBreak(): Result<Unit, ClockActionError>
    suspend fun endBreak(): Result<Unit, ClockActionError>

    suspend fun syncPendingEvents(): EmptyResult<DataError.Remote>

    suspend fun requestCorrection(
        clockEventId: String,
        reason: String
    ): Result<Unit, RequestCorrectionError>
}
