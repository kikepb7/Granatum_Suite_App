package com.granatum.feature.clockin.domain.usecase

import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.feature.clockin.domain.repository.ClockInRepository

class SyncPendingClockEventsUseCase(private val repository: ClockInRepository) {
    suspend operator fun invoke(): EmptyResult<DataError.Remote> = repository.syncPendingEvents()
}
