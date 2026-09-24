package com.granatum.feature.clockin.domain.usecase

import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.Result.Failure
import com.granatum.feature.clockin.domain.model.RequestCorrectionError
import com.granatum.feature.clockin.domain.model.RequestCorrectionError.BlankReason
import com.granatum.feature.clockin.domain.repository.ClockInRepository

class RequestCorrectionUseCase(private val repository: ClockInRepository) {
    suspend operator fun invoke(clockEventId: String, reason: String): Result<Unit, RequestCorrectionError> {
        if (reason.isBlank()) return Failure(error = BlankReason)
        return repository.requestCorrection(clockEventId = clockEventId, reason = reason.trim())
    }
}
