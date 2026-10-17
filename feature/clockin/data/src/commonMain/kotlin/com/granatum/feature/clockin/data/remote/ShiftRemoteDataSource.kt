package com.granatum.feature.clockin.data.remote

import com.granatum.core.data.auth.errorBody
import com.granatum.core.data.networking.constructRoute
import com.granatum.core.data.networking.platformSafeCall
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Error
import com.granatum.core.domain.util.Result
import com.granatum.feature.clockin.data.dto.BreakEndRequestDto
import com.granatum.feature.clockin.data.dto.BreakStartRequestDto
import com.granatum.feature.clockin.data.dto.ClockInRequestDto
import com.granatum.feature.clockin.data.dto.ClockOutRequestDto
import com.granatum.feature.clockin.data.dto.CreateShiftCorrectionRequestDto
import com.granatum.feature.clockin.data.dto.MonthlyShiftSummaryDto
import com.granatum.feature.clockin.data.dto.ShiftCorrectionDto
import com.granatum.feature.clockin.data.dto.ShiftDto
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** What happened to a punch sent to the server (specs/005-fichaje-real, research D4). */
sealed interface SendOutcome {
    data class Registered(val shift: ShiftDto) : SendOutcome

    /** No coverage, a timeout, 5xx or 429: try again later, in the same order. */
    data object Temporary : SendOutcome

    /** Refused with this `code`. The caller decides what that means. */
    data class Refused(val code: String) : SendOutcome
}

/** A non-2xx answer: the `code` of the backend's `{code, message}` body, if it had one. */
data class RemoteFailure(val transport: DataError.Remote?, val code: String?) : Error

/**
 * The time register routes of docs/openapi.json (the English contract, docs/english-naming.md),
 * as specs/005-fichaje-real/contracts/fichaje-api.md uses them.
 */
class ShiftRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun send(event: ClockEventEntity, serverShiftId: String?): SendOutcome {
        val occurredAt = formatOccurredAt(event.clientTimestampEpochMillis)
        val response = exchange {
            when (event.type) {
                "CLOCK_IN" -> httpClient.post(constructRoute("/shifts/clock-in")) {
                    setBody(ClockInRequestDto(event.id, occurredAt))
                }
                "BREAK_START" -> httpClient.post(constructRoute("/shifts/${requireNotNull(serverShiftId)}/break/start")) {
                    // Never null after migration 2 -> 3; OTHER rather than a crash that would block the queue.
                    setBody(BreakStartRequestDto(event.id, occurredAt, event.breakType ?: "OTHER"))
                }
                "BREAK_END" -> httpClient.post(constructRoute("/shifts/${requireNotNull(serverShiftId)}/break/end")) {
                    setBody(BreakEndRequestDto(event.id, occurredAt))
                }
                else -> httpClient.post(constructRoute("/shifts/${requireNotNull(serverShiftId)}/clock-out")) {
                    setBody(ClockOutRequestDto(event.id, occurredAt))
                }
            }
        }
        val ok = (response as? Result.Success)?.data ?: return SendOutcome.Temporary
        return when {
            ok.status.isSuccess() -> runCatching { SendOutcome.Registered(ok.body<ShiftDto>()) }
                .getOrElse { SendOutcome.Refused(UNREADABLE_RESPONSE) }
            ok.status.value >= 500 || ok.status == HttpStatusCode.TooManyRequests -> SendOutcome.Temporary
            // A 401 that is still a 401 here survived the HTTP client's refresh attempt: the
            // session is gone or there is no coverage for the refresh. Either way, not this
            // punch's fault — keep it and try later.
            ok.status == HttpStatusCode.Unauthorized -> SendOutcome.Temporary
            else -> SendOutcome.Refused(ok.errorBody()?.code ?: "HTTP_${ok.status.value}")
        }
    }

    /** The person's shifts whose entry falls between two civil dates, interpreted in Madrid by the server. */
    suspend fun fetchShifts(employeeId: String, from: LocalDate, to: LocalDate): Result<List<ShiftDto>, RemoteFailure> =
        getJson("/shifts/employee/$employeeId") {
            parameter("from", from.toString())
            parameter("to", to.toString())
        }

    suspend fun fetchMonthSummary(employeeId: String, year: Int, month: Int): Result<MonthlyShiftSummaryDto, RemoteFailure> =
        getJson("/shifts/employee/$employeeId/summary") {
            parameter("year", year)
            parameter("month", month)
        }

    suspend fun fetchCorrections(shiftServerId: String): Result<List<ShiftCorrectionDto>, RemoteFailure> =
        getJson("/shifts/$shiftServerId/corrections") {}

    suspend fun createCorrection(shiftServerId: String, request: CreateShiftCorrectionRequestDto): Result<ShiftCorrectionDto, RemoteFailure> {
        val sent = exchange {
            httpClient.post(constructRoute("/shifts/$shiftServerId/corrections")) { setBody(request) }
        }
        return sent.decode()
    }

    private suspend inline fun <reified T> getJson(
        path: String,
        crossinline params: io.ktor.client.request.HttpRequestBuilder.() -> Unit
    ): Result<T, RemoteFailure> = exchange { httpClient.get(constructRoute(path)) { params() } }.decode()

    private suspend inline fun <reified T> Result<HttpResponse, RemoteFailure>.decode(): Result<T, RemoteFailure> {
        val response = when (this) {
            is Result.Failure -> return this
            is Result.Success -> data
        }
        if (!response.status.isSuccess()) return Result.Failure(RemoteFailure(null, response.errorBody()?.code))
        return runCatching { Result.Success(response.body<T>()) }
            .getOrElse { Result.Failure(RemoteFailure(DataError.Remote.SERIALIZATION, null)) }
    }

    private suspend fun exchange(request: suspend () -> HttpResponse): Result<HttpResponse, RemoteFailure> {
        var response: HttpResponse? = null
        val sent = platformSafeCall(execute = request) { received ->
            response = received
            Result.Success(Unit)
        }
        return when (sent) {
            is Result.Failure -> Result.Failure(RemoteFailure(sent.error, null))
            is Result.Success -> Result.Success(response!!)
        }
    }

    companion object {
        const val UNREADABLE_RESPONSE = "UNREADABLE_RESPONSE"

        /**
         * Always the same text for the same instant, because a retried punch must send exactly
         * the same body or the server answers CLIENT_EVENT_ID_REUSED (research D5).
         */
        fun formatOccurredAt(epochMillis: Long): String = Instant.fromEpochMilliseconds(epochMillis).toString()
    }
}
