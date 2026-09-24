package com.granatum.feature.clockin.data.datasource.remote

import com.granatum.core.data.networking.get
import com.granatum.core.data.networking.post
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.asEmptyResult
import com.granatum.core.domain.util.onSuccess
import com.granatum.feature.clockin.data.dto.CorrectionDecisionRequestDto
import com.granatum.feature.clockin.data.dto.CorrectionRequestDto
import com.granatum.feature.clockin.data.dto.EmployeeAttendanceSummaryDto
import com.granatum.feature.clockin.data.mappers.toDomain
import com.granatum.feature.clockin.domain.model.CorrectionRequestModel
import com.granatum.feature.clockin.domain.model.EmployeeAttendanceSummaryModel
import com.granatum.feature.clockin.domain.repository.TeamAttendanceRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Online-only (see `TeamAttendanceRepository` for why): reads are cached in
 * memory just long enough to drive the admin screen's Flow-based state,
 * refreshed explicitly via [refresh] rather than through Room.
 */
class KtorTeamAttendanceRepositoryImpl(
    private val httpClient: HttpClient
) : TeamAttendanceRepository {

    private val summaries = MutableStateFlow<List<EmployeeAttendanceSummaryModel>>(emptyList())
    private val corrections = MutableStateFlow<List<CorrectionRequestModel>>(emptyList())

    override fun observeTeamSummaries(from: LocalDate, to: LocalDate): Flow<List<EmployeeAttendanceSummaryModel>> =
        summaries.asStateFlow()

    override fun observePendingCorrections(): Flow<List<CorrectionRequestModel>> = corrections.asStateFlow()

    override suspend fun refresh(from: LocalDate, to: LocalDate): EmptyResult<DataError.Remote> {
        val summariesResult = httpClient.get<List<EmployeeAttendanceSummaryDto>>(
            route = "/attendance/team-summary",
            queryParams = mapOf("from" to from.toString(), "to" to to.toString())
        ).onSuccess { dtos -> summaries.value = dtos.map { it.toDomain() } }

        val correctionsResult = httpClient.get<List<CorrectionRequestDto>>(
            route = "/attendance/corrections",
            queryParams = mapOf("status" to "PENDING")
        ).onSuccess { dtos -> corrections.value = dtos.map { it.toDomain() } }

        return when (summariesResult) {
            is Result.Failure -> summariesResult.asEmptyResult()
            is Result.Success -> correctionsResult.asEmptyResult()
        }
    }

    override suspend fun approveCorrection(correctionId: String): EmptyResult<DataError.Remote> =
        httpClient.post<Unit, Unit>(route = "/attendance/corrections/$correctionId/approve", body = Unit)
            .onSuccess { corrections.value = corrections.value.filterNot { it.id == correctionId } }
            .asEmptyResult()

    override suspend fun rejectCorrection(correctionId: String, note: String): EmptyResult<DataError.Remote> =
        httpClient.post<CorrectionDecisionRequestDto, Unit>(
            route = "/attendance/corrections/$correctionId/reject",
            body = CorrectionDecisionRequestDto(note = note)
        )
            .onSuccess { corrections.value = corrections.value.filterNot { it.id == correctionId } }
            .asEmptyResult()
}
