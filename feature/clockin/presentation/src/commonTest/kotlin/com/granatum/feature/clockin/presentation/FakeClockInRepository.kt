package com.granatum.feature.clockin.presentation

import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.domain.model.BreakType
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.model.CorrectionDraft
import com.granatum.feature.clockin.domain.model.CorrectionError
import com.granatum.feature.clockin.domain.model.CorrectionModel
import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.model.TodayState
import com.granatum.feature.clockin.domain.repository.ClockInRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeClockInRepository : ClockInRepository {
    val today = MutableStateFlow(TodayState())
    val shifts = MutableStateFlow<List<ShiftModel>>(emptyList())
    val calls = mutableListOf<String>()
    val refreshed = mutableListOf<Pair<Int, Int>>()
    var gate: CompletableDeferred<Unit>? = null
    var actionResult: Result<Unit, ClockActionError> = Result.Success(Unit)
    var correctionResult: Result<CorrectionModel, CorrectionError> = Result.Failure(CorrectionError.Unknown)
    val drafts = mutableListOf<CorrectionDraft>()

    override fun observeToday(): Flow<TodayState> = today
    override fun observeMonth(year: Int, month: Int): Flow<List<ShiftModel>> = shifts
    override fun observeShift(key: String): Flow<ShiftModel?> = shifts.map { all -> all.firstOrNull { it.key == key } }
    override fun observePendingSyncCount(): Flow<Int> = today.map { it.pendingCount }
    override suspend fun refreshMonth(year: Int, month: Int) { refreshed += year to month }

    private suspend fun act(name: String): Result<Unit, ClockActionError> { calls += name; gate?.await(); return actionResult }
    override suspend fun clockIn() = act("clockIn")
    override suspend fun clockOut() = act("clockOut")
    override suspend fun startBreak(type: BreakType) = act("startBreak:$type")
    override suspend fun endBreak() = act("endBreak")
    override suspend fun syncNow() = Unit
    override suspend fun getCorrections(shiftServerId: String): Result<List<CorrectionModel>, CorrectionError> = Result.Success(emptyList())
    override suspend fun requestCorrection(shiftServerId: String, draft: CorrectionDraft): Result<CorrectionModel, CorrectionError> {
        drafts += draft; return correctionResult
    }
}
