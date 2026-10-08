package com.granatum.feature.clockin.domain.usecase

import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.domain.model.CorrectionDraft
import com.granatum.feature.clockin.domain.model.CorrectionError
import com.granatum.feature.clockin.domain.model.CorrectionModel
import com.granatum.feature.clockin.domain.model.CorrectionValidation
import com.granatum.feature.clockin.domain.repository.ClockInRepository

/** Validates before sending what the server would reject as VALORES_INCOHERENTES (FR-020). */
class RequestCorrectionUseCase(private val repository: ClockInRepository) {
    suspend operator fun invoke(shiftServerId: String, draft: CorrectionDraft): Result<CorrectionModel, CorrectionError> {
        val issues = CorrectionValidation.validate(draft)
        if (issues.isNotEmpty()) return Result.Failure(CorrectionError.Invalid(issues))
        return repository.requestCorrection(shiftServerId, draft.copy(reason = draft.reason.trim()))
    }
}

class GetCorrectionsUseCase(private val repository: ClockInRepository) {
    suspend operator fun invoke(shiftServerId: String): Result<List<CorrectionModel>, CorrectionError> =
        repository.getCorrections(shiftServerId)
}
