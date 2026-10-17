package com.granatum.feature.invoicing.data.remote

import com.granatum.core.data.auth.errorBody
import com.granatum.core.data.networking.constructRoute
import com.granatum.core.data.networking.platformSafeCall
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result
import com.granatum.feature.invoicing.data.dto.CompanyDto
import com.granatum.feature.invoicing.data.dto.CompanyRequestDto
import com.granatum.feature.invoicing.data.dto.HistoryDto
import com.granatum.feature.invoicing.data.dto.InvoiceDto
import com.granatum.feature.invoicing.data.dto.InvoiceRequestDto
import com.granatum.feature.invoicing.data.dto.InvoiceSummaryPageDto
import com.granatum.feature.invoicing.data.dto.QuarterStatusDto
import com.granatum.feature.invoicing.data.dto.ReopenRequestDto
import com.granatum.feature.invoicing.data.dto.ReportDto
import com.granatum.feature.invoicing.data.dto.UploadResultDto
import com.granatum.feature.invoicing.data.dto.VersionRequestDto
import com.granatum.feature.invoicing.domain.model.InvoicingError
import com.granatum.feature.invoicing.domain.model.InvoicingFile
import com.granatum.feature.invoicing.domain.model.UploadDocument
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentDisposition
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/** The /api/invoicing routes of docs/openapi.json (backend 93ef398); errors per research D10. */
class InvoicingRemoteDataSource(
    private val http: HttpClient,
) {
    suspend fun invoices(
        status: String?,
        type: String?,
        from: String?,
        to: String?,
        party: String?,
        page: Int,
        size: Int,
    ) = call<InvoiceSummaryPageDto> {
        http.get(constructRoute("$BASE/invoices")) {
            status?.let { parameter("status", it) }
            type?.let { parameter("type", it) }
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
            party?.let { parameter("party", it) }
            parameter("page", page)
            parameter("size", size)
        }
    }

    suspend fun invoice(id: String) = call<InvoiceDto> { http.get(constructRoute("$BASE/invoices/$id")) }

    /** One `files` part per document, each with its own type and name (research D9). */
    suspend fun upload(documents: List<UploadDocument>) =
        call<List<UploadResultDto>> {
            http.submitFormWithBinaryData(
                url = constructRoute("$BASE/invoices"),
                formData =
                    formData {
                        documents.forEach { document ->
                            append(
                                "files",
                                document.bytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, document.mimeType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"${document.fileName.safeFileName()}\"")
                                },
                            )
                        }
                    },
            ) { longTransfer() }
        }

    suspend fun save(
        id: String,
        body: InvoiceRequestDto,
    ) = call<InvoiceDto> {
        http.put(constructRoute("$BASE/invoices/$id")) { setBody(body) }
    }

    suspend fun confirm(
        id: String,
        version: Int,
    ) = call<InvoiceDto> {
        http.post(constructRoute("$BASE/invoices/$id/confirm")) { setBody(VersionRequestDto(version)) }
    }

    suspend fun discard(
        id: String,
        version: Int,
    ) = call<InvoiceDto> {
        http.post(constructRoute("$BASE/invoices/$id/discard")) { setBody(VersionRequestDto(version)) }
    }

    suspend fun recognize(id: String) = call<Unit> { http.post(constructRoute("$BASE/invoices/$id/recognize")) }

    suspend fun history(id: String) = call<HistoryDto> { http.get(constructRoute("$BASE/invoices/$id/history")) }

    suspend fun original(id: String) = file("factura-$id") { http.get(constructRoute("$BASE/invoices/$id/original")) { longTransfer() } }

    suspend fun quarters(year: Int) =
        call<List<QuarterStatusDto>> {
            http.get(constructRoute("$BASE/quarters")) { parameter("year", year) }
        }

    suspend fun closeQuarter(
        year: Int,
        quarter: Int,
    ) = call<QuarterStatusDto> {
        http.post(constructRoute("$BASE/quarters/$year/$quarter/close"))
    }

    suspend fun reopenQuarter(
        year: Int,
        quarter: Int,
        reason: String,
    ) = call<QuarterStatusDto> {
        http.post(
            constructRoute("$BASE/quarters/$year/$quarter/reopen"),
        ) { setBody(ReopenRequestDto(reason)) }
    }

    suspend fun report(params: ReportParams) = call<ReportDto> { http.get(constructRoute("$BASE/reports")) { report(params, "json") } }

    suspend fun reportFile(
        params: ReportParams,
        format: String,
    ) = file("reporte-facturacion") {
        http.get(constructRoute("$BASE/reports")) {
            report(params, format)
            longTransfer()
        }
    }

    suspend fun company() = call<CompanyDto> { http.get(constructRoute("$BASE/company")) }

    suspend fun saveCompany(body: CompanyRequestDto) = call<CompanyDto> { http.put(constructRoute("$BASE/company")) { setBody(body) } }

    data class ReportParams(
        val period: String,
        val year: Int,
        val month: Int?,
        val quarter: Int?,
    )

    private fun HttpRequestBuilder.report(
        params: ReportParams,
        format: String,
    ) {
        parameter("period", params.period)
        parameter("year", params.year)
        params.month?.let { parameter("month", it) }
        params.quarter?.let { parameter("quarter", it) }
        parameter("format", format)
    }

    /** Uploads of up to 50 MB and downloads of whole documents outlast the default 20 s. */
    private fun HttpRequestBuilder.longTransfer() =
        timeout {
            requestTimeoutMillis = TRANSFER_TIMEOUT_MILLIS
            socketTimeoutMillis = TRANSFER_TIMEOUT_MILLIS
        }

    private suspend fun file(
        fallbackName: String,
        request: suspend () -> HttpResponse,
    ): Result<InvoicingFile, InvoicingError> =
        when (val response = send(request)) {
            is Result.Failure -> response
            is Result.Success ->
                runCatching {
                    val raw = response.data
                    val mime = raw.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim() ?: "application/octet-stream"
                    val name =
                        raw.headers[HttpHeaders.ContentDisposition]
                            ?.let { ContentDisposition.parse(it).parameter(ContentDisposition.Parameters.FileName) }
                            ?: "$fallbackName.${extensionFor(mime)}"
                    Result.Success(InvoicingFile(raw.readRawBytes(), name, mime))
                }.getOrElse { Result.Failure(InvoicingError.Unknown) }
        }

    private suspend inline fun <reified T> call(noinline request: suspend () -> HttpResponse): Result<T, InvoicingError> =
        when (val response = send(request)) {
            is Result.Failure -> response
            is Result.Success -> {
                if (T::class == Unit::class) {
                    @Suppress("UNCHECKED_CAST")
                    Result.Success(Unit as T)
                } else {
                    runCatching { Result.Success(response.data.body<T>()) }.getOrElse { Result.Failure(InvoicingError.Unknown) }
                }
            }
        }

    private suspend fun send(request: suspend () -> HttpResponse): Result<HttpResponse, InvoicingError> {
        var response: HttpResponse? = null
        val sent =
            platformSafeCall(execute = request) { received ->
                response = received
                Result.Success(Unit)
            }
        return when (sent) {
            is Result.Failure ->
                Result.Failure(
                    if (sent.error == DataError.Remote.NO_INTERNET || sent.error == DataError.Remote.REQUEST_TIMEOUT) {
                        InvoicingError.NoInternet
                    } else {
                        InvoicingError.Unknown
                    },
                )
            is Result.Success -> {
                val received = response!!
                if (received.status.isSuccess()) Result.Success(received) else Result.Failure(received.toInvoicingError())
            }
        }
    }

    private suspend fun HttpResponse.toInvoicingError(): InvoicingError {
        val body = errorBody()
        return when (body?.code) {
            "COMPANY_NOT_CONFIGURED" -> InvoicingError.CompanyNotConfigured
            "INVALID_NIF" -> InvoicingError.InvalidTaxId
            "INVOICE_NOT_FOUND" -> InvoicingError.NotFound
            "STALE_VERSION" -> InvoicingError.StaleVersion
            "STATUS_NOT_ALLOWED" -> InvoicingError.StateNotAllowed
            "QUARTER_CLOSED" -> InvoicingError.QuarterClosed
            "QUARTER_OPEN" -> InvoicingError.QuarterOpen
            "QUARTER_HAS_PENDING" -> InvoicingError.QuarterHasPending(body.message)
            "DUPLICATE_INVOICE" -> InvoicingError.Duplicate
            "INCONSISTENT_INVOICE" -> InvoicingError.Incoherent(body.message)
            "RECOGNITION_UNAVAILABLE" -> InvoicingError.RecognitionUnavailable
            "INVALID_PERIOD" -> InvoicingError.InvalidPeriod
            "VALIDATION" -> InvoicingError.Invalid
            "FILE_TOO_LARGE", "REQUEST_TOO_LARGE" -> InvoicingError.TooLarge
            "TOO_MANY_REQUESTS" -> InvoicingError.RateLimited
            else ->
                when (status) {
                    HttpStatusCode.Forbidden -> InvoicingError.Forbidden
                    HttpStatusCode.NotFound -> InvoicingError.NotFound
                    HttpStatusCode.PayloadTooLarge -> InvoicingError.TooLarge
                    HttpStatusCode.TooManyRequests -> InvoicingError.RateLimited
                    else -> InvoicingError.Unknown
                }
        }
    }

    private companion object {
        const val BASE = "/invoicing"
        const val TRANSFER_TIMEOUT_MILLIS = 120_000L

        fun String.safeFileName() = replace(Regex("[\"\\\\\r\n]"), "_")

        fun extensionFor(mime: String) =
            when (mime) {
                "application/pdf" -> "pdf"
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                "text/csv" -> "csv"
                else -> "bin"
            }
    }
}
