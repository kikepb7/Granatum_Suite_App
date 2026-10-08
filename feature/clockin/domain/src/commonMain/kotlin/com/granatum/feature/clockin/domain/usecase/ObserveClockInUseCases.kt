package com.granatum.feature.clockin.domain.usecase

import com.granatum.feature.clockin.domain.model.ShiftModel
import com.granatum.feature.clockin.domain.model.TodayState
import com.granatum.feature.clockin.domain.repository.ClockInRepository
import kotlinx.coroutines.flow.Flow

class ObserveTodayUseCase(private val repository: ClockInRepository) {
    operator fun invoke(): Flow<TodayState> = repository.observeToday()
}

class ObservePendingSyncCountUseCase(private val repository: ClockInRepository) {
    operator fun invoke(): Flow<Int> = repository.observePendingSyncCount()
}

class ObserveMonthUseCase(private val repository: ClockInRepository) {
    operator fun invoke(year: Int, month: Int): Flow<List<ShiftModel>> = repository.observeMonth(year, month)
}

class RefreshMonthUseCase(private val repository: ClockInRepository) {
    suspend operator fun invoke(year: Int, month: Int) = repository.refreshMonth(year, month)
}

class ObserveShiftUseCase(private val repository: ClockInRepository) {
    operator fun invoke(key: String): Flow<ShiftModel?> = repository.observeShift(key)
}
