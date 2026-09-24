package com.granatum.feature.clockin.domain.usecase

import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.domain.model.ClockActionError
import com.granatum.feature.clockin.domain.repository.ClockInRepository

class ClockInUseCase(private val repository: ClockInRepository) {
    suspend operator fun invoke(): Result<Unit, ClockActionError> = repository.clockIn()
}

class ClockOutUseCase(private val repository: ClockInRepository) {
    suspend operator fun invoke(): Result<Unit, ClockActionError> = repository.clockOut()
}

class StartBreakUseCase(private val repository: ClockInRepository) {
    suspend operator fun invoke(): Result<Unit, ClockActionError> = repository.startBreak()
}

class EndBreakUseCase(private val repository: ClockInRepository) {
    suspend operator fun invoke(): Result<Unit, ClockActionError> = repository.endBreak()
}
