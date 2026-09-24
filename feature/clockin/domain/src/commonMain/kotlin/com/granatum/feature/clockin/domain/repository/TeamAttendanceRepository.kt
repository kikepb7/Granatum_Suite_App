package com.granatum.feature.clockin.domain.repository

import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.domain.model.CorrectionRequestModel
import com.granatum.feature.clockin.domain.model.EmployeeAttendanceSummaryModel
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * ADMIN/ENCARGADO-only. Online-only by design: an admin reviewing hours or
 * approving a correction is expected to have connectivity, and there is
 * nothing here the employee-facing offline queue needs to feed into.
 */
interface TeamAttendanceRepository {
    fun observeTeamSummaries(from: LocalDate, to: LocalDate): Flow<List<EmployeeAttendanceSummaryModel>>
    fun observePendingCorrections(): Flow<List<CorrectionRequestModel>>

    suspend fun refresh(from: LocalDate, to: LocalDate): EmptyResult<DataError.Remote>
    suspend fun approveCorrection(correctionId: String): EmptyResult<DataError.Remote>
    suspend fun rejectCorrection(correctionId: String, note: String): EmptyResult<DataError.Remote>
}
