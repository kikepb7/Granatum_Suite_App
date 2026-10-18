package com.granatum.feature.sheets.data.remote

import com.granatum.core.data.networking.constructRoute
import com.granatum.core.data.networking.platformSafeCall
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result
import com.granatum.feature.sheets.data.dto.SheetQueueStatusDto
import com.granatum.feature.sheets.data.dto.SheetRetryAllDto
import com.granatum.feature.sheets.data.dto.SheetSettingsDto
import com.granatum.feature.sheets.data.dto.SheetTaskCreatedDto
import com.granatum.feature.sheets.data.dto.SheetTaskDto
import com.granatum.feature.sheets.data.dto.SheetsErrorBodyDto
import com.granatum.feature.sheets.data.dto.UpdateSheetSettingsRequestDto
import com.granatum.feature.sheets.domain.model.SheetAccessProblem
import com.granatum.feature.sheets.domain.model.SheetsError
import com.granatum.feature.sheets.domain.model.SpreadsheetField
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/** The `/api/sheets` routes of the backend spec 012 contract, ADMIN only. */
internal class SheetsRemoteDataSource(
    private val http: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun settings() = call<SheetSettingsDto> { http.get(constructRoute("$BASE/settings")) }

    suspend fun saveSettings(body: UpdateSheetSettingsRequestDto) =
        call<SheetSettingsDto> { http.put(constructRoute("$BASE/settings")) { setBody(body) } }

    suspend fun exportShiftMonth(
        year: Int,
        month: Int,
    ) = call<SheetTaskCreatedDto> { http.post(constructRoute("$BASE/shifts/$year/$month")) }

    suspend fun resyncInvoiceMonth(
        year: Int,
        month: Int,
    ) = call<SheetTaskCreatedDto> { http.post(constructRoute("$BASE/invoices/$year/$month/resync")) }

    suspend fun task(id: String) = call<SheetTaskDto> { http.get(constructRoute("$BASE/tasks/$id")) }

    suspend fun status() = call<SheetQueueStatusDto> { http.get(constructRoute("$BASE/status")) }

    suspend fun retry(id: String) = call<SheetTaskDto> { http.post(constructRoute("$BASE/tasks/$id/retry")) }

    suspend fun retryAllFailed() = call<SheetRetryAllDto> { http.post(constructRoute("$BASE/tasks/retry-failed")) }

    private suspend inline fun <reified T> call(noinline request: suspend () -> HttpResponse): Result<T, SheetsError> {
        var response: HttpResponse? = null
        val sent =
            platformSafeCall(execute = request) { received ->
                response = received
                Result.Success(Unit)
            }
        val received =
            when (sent) {
                is Result.Failure -> return Result.Failure(
                    if (sent.error == DataError.Remote.NO_INTERNET || sent.error == DataError.Remote.REQUEST_TIMEOUT) {
                        SheetsError.NoInternet
                    } else {
                        SheetsError.Unknown
                    },
                )
                is Result.Success -> response!!
            }
        if (!received.status.isSuccess()) return Result.Failure(received.toSheetsError())
        return runCatching { Result.Success(received.body<T>()) }.getOrElse { Result.Failure(SheetsError.Unknown) }
    }

    private suspend fun HttpResponse.toSheetsError(): SheetsError {
        val body = runCatching { json.decodeFromString<SheetsErrorBodyDto>(bodyAsText()) }.getOrNull()
        return when (body?.code) {
            "SPREADSHEET_NOT_ACCESSIBLE" ->
                SheetsError.NotAccessible(
                    field = if (body.spreadsheet == "invoices") SpreadsheetField.INVOICES else SpreadsheetField.SHIFTS,
                    reason = SheetAccessProblem.entries.firstOrNull { it.name == body.reason } ?: SheetAccessProblem.NOT_SHARED,
                )
            "VALIDATION" -> SheetsError.InvalidLink
            "SAME_SPREADSHEET" -> SheetsError.SameSpreadsheet
            "SHEETS_NOT_CONFIGURED" -> SheetsError.NotConfigured
            "SHEETS_UNAVAILABLE" -> SheetsError.Unavailable
            "SHEETS_UNREACHABLE" -> SheetsError.Unreachable
            "INVALID_PERIOD" -> SheetsError.InvalidPeriod
            "TASK_NOT_FOUND" -> SheetsError.TaskNotFound
            "TASK_NOT_FAILED" -> SheetsError.TaskNotFailed
            else ->
                when (status) {
                    HttpStatusCode.Forbidden -> SheetsError.Forbidden
                    HttpStatusCode.BadGateway -> SheetsError.Unreachable
                    else -> SheetsError.Unknown
                }
        }
    }

    private companion object {
        const val BASE = "/sheets"
    }
}
