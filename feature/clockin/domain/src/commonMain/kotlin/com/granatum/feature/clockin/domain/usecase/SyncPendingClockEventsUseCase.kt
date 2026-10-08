package com.granatum.feature.clockin.domain.usecase

import com.granatum.feature.clockin.domain.repository.ClockInRepository

class SyncPendingClockEventsUseCase(private val repository: ClockInRepository) {
    suspend operator fun invoke() = repository.syncNow()
}
