package com.granatum.feature.clockin.domain.usecase

import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.feature.clockin.domain.model.CorrectionRequestModel
import com.granatum.feature.clockin.domain.model.EmployeeAttendanceSummaryModel
import com.granatum.feature.clockin.domain.repository.TeamAttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetTeamAttendanceUseCase(private val repository: TeamAttendanceRepository) {
    operator fun invoke(from: LocalDate, to: LocalDate): Flow<List<EmployeeAttendanceSummaryModel>> =
        repository.observeTeamSummaries(from = from, to = to)
}

class GetPendingCorrectionsUseCase(private val repository: TeamAttendanceRepository) {
    operator fun invoke(): Flow<List<CorrectionRequestModel>> = repository.observePendingCorrections()
}

class RefreshTeamAttendanceUseCase(private val repository: TeamAttendanceRepository) {
    suspend operator fun invoke(from: LocalDate, to: LocalDate): EmptyResult<DataError.Remote> =
        repository.refresh(from = from, to = to)
}

class ApproveCorrectionUseCase(private val repository: TeamAttendanceRepository) {
    suspend operator fun invoke(correctionId: String): EmptyResult<DataError.Remote> =
        repository.approveCorrection(correctionId = correctionId)
}

class RejectCorrectionUseCase(private val repository: TeamAttendanceRepository) {
    suspend operator fun invoke(correctionId: String, note: String): EmptyResult<DataError.Remote> =
        repository.rejectCorrection(correctionId = correctionId, note = note)
}
