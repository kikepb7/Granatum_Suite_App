package com.granatum.feature.clockin.domain.usecase

import com.granatum.feature.clockin.domain.model.ClockEventModel
import com.granatum.feature.clockin.domain.model.DailyAttendanceSummary
import com.granatum.feature.clockin.domain.model.ShiftStatus
import com.granatum.feature.clockin.domain.repository.ClockInRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class ObserveShiftStatusUseCase(private val repository: ClockInRepository) {
    operator fun invoke(): Flow<ShiftStatus> = repository.observeCurrentStatus()
}

class ObserveTodayEventsUseCase(private val repository: ClockInRepository) {
    operator fun invoke(): Flow<List<ClockEventModel>> = repository.observeTodayEvents()
}

class ObservePendingSyncCountUseCase(private val repository: ClockInRepository) {
    operator fun invoke(): Flow<Int> = repository.observePendingSyncCount()
}

class GetAttendanceHistoryUseCase(private val repository: ClockInRepository) {
    operator fun invoke(from: LocalDate, to: LocalDate): Flow<List<DailyAttendanceSummary>> =
        repository.observeHistory(from = from, to = to)
}
