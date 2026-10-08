package com.granatum.feature.invoicing.data.remote

import com.granatum.core.data.auth.errorBody
import com.granatum.core.data.networking.constructRoute
import com.granatum.core.data.networking.platformSafeCall
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result
import com.granatum.feature.invoicing.data.dto.EmpresaDto
import com.granatum.feature.invoicing.data.dto.EmpresaRequestDto
import com.granatum.feature.invoicing.data.dto.EstadoTrimestreDto
import com.granatum.feature.invoicing.data.dto.FacturaDto
import com.granatum.feature.invoicing.data.dto.FacturaRequestDto
import com.granatum.feature.invoicing.data.dto.HistorialDto
import com.granatum.feature.invoicing.data.dto.PaginaResumenFacturaDto
import com.granatum.feature.invoicing.data.dto.ReaperturaRequestDto
import com.granatum.feature.invoicing.data.dto.ReporteDto
import com.granatum.feature.invoicing.data.dto.ResultadoSubidaDto
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

/** The /api/facturacion routes of docs/openapi.json (backend d1857ad); errors per research D10. */
class InvoicingRemoteDataSource(
    private val http: HttpClient,
) {
    suspend fun invoices(
        estado: String?,
        tipo: String?,
        desde: String?,
        hasta: String?,
        parte: String?,
        pagina: Int,
        tamano: Int,
    ) = call<PaginaResumenFacturaDto> {
        http.get(constructRoute("$BASE/facturas")) {
            estado?.let { parameter("estado", it) }
            tipo?.let { parameter("tipo", it) }
            desde?.let { parameter("desde", it) }
            hasta?.let { parameter("hasta", it) }
            parte?.let { parameter("parte", it) }
            parameter("pagina", pagina)
            parameter("tamano", tamano)
        }
    }

    suspend fun invoice(id: String) = call<FacturaDto> { http.get(constructRoute("$BASE/facturas/$id")) }

    /** One `ficheros` part per document, each with its own type and name (research D9). */
    suspend fun upload(documents: List<UploadDocument>) =
        call<List<ResultadoSubidaDto>> {
            http.submitFormWithBinaryData(
                url = constructRoute("$BASE/facturas"),
                formData =
                    formData {
                        documents.forEach { document ->
                            append(
                                "ficheros",
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
        body: FacturaRequestDto,
    ) = call<FacturaDto> {
        http.put(constructRoute("$BASE/facturas/$id")) { setBody(body) }
    }

    suspend fun confirm(
        id: String,
        version: Int,
    ) = call<FacturaDto> {
        http.post(constructRoute("$BASE/facturas/$id/confirmar")) { setBody(VersionRequestDto(version)) }
    }

    suspend fun discard(
        id: String,
        version: Int,
    ) = call<FacturaDto> {
        http.post(constructRoute("$BASE/facturas/$id/descartar")) { setBody(VersionRequestDto(version)) }
    }

    suspend fun recognize(id: String) = call<Unit> { http.post(constructRoute("$BASE/facturas/$id/reconocer")) }

    suspend fun history(id: String) = call<HistorialDto> { http.get(constructRoute("$BASE/facturas/$id/historial")) }

    suspend fun original(id: String) = file("factura-$id") { http.get(constructRoute("$BASE/facturas/$id/original")) { longTransfer() } }

    suspend fun quarters(year: Int) =
        call<List<EstadoTrimestreDto>> {
            http.get(constructRoute("$BASE/trimestres")) { parameter("anio", year) }
        }

    suspend fun closeQuarter(
        year: Int,
        quarter: Int,
    ) = call<EstadoTrimestreDto> {
        http.post(constructRoute("$BASE/trimestres/$year/$quarter/cerrar"))
    }

    suspend fun reopenQuarter(
        year: Int,
        quarter: Int,
        reason: String,
    ) = call<EstadoTrimestreDto> {
        http.post(
            constructRoute("$BASE/trimestres/$year/$quarter/reabrir"),
        ) { setBody(ReaperturaRequestDto(reason)) }
    }

    suspend fun report(params: ReportParams) = call<ReporteDto> { http.get(constructRoute("$BASE/reportes")) { report(params, "json") } }

    suspend fun reportFile(
        params: ReportParams,
        format: String,
    ) = file("reporte-facturacion") {
        http.get(constructRoute("$BASE/reportes")) {
            report(params, format)
            longTransfer()
        }
    }

    suspend fun company() = call<EmpresaDto> { http.get(constructRoute("$BASE/empresa")) }

    suspend fun saveCompany(body: EmpresaRequestDto) = call<EmpresaDto> { http.put(constructRoute("$BASE/empresa")) { setBody(body) } }

    data class ReportParams(
        val periodo: String,
        val anio: Int,
        val mes: Int?,
        val trimestre: Int?,
    )

    private fun HttpRequestBuilder.report(
        params: ReportParams,
        format: String,
    ) {
        parameter("periodo", params.periodo)
        parameter("anio", params.anio)
        params.mes?.let { parameter("mes", it) }
        params.trimestre?.let { parameter("trimestre", it) }
        parameter("formato", format)
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
            "EMPRESA_SIN_CONFIGURAR" -> InvoicingError.CompanyNotConfigured
            "NIF_INVALIDO" -> InvoicingError.InvalidTaxId
            "FACTURA_NOT_FOUND" -> InvoicingError.NotFound
            "VERSION_DESACTUALIZADA" -> InvoicingError.StaleVersion
            "ESTADO_NO_PERMITIDO" -> InvoicingError.StateNotAllowed
            "TRIMESTRE_CERRADO" -> InvoicingError.QuarterClosed
            "TRIMESTRE_ABIERTO" -> InvoicingError.QuarterOpen
            "TRIMESTRE_CON_PENDIENTES" -> InvoicingError.QuarterHasPending(body.message)
            "FACTURA_DUPLICADA" -> InvoicingError.Duplicate
            "FACTURA_INCOHERENTE" -> InvoicingError.Incoherent(body.message)
            "RECONOCIMIENTO_NO_DISPONIBLE" -> InvoicingError.RecognitionUnavailable
            "PERIODO_INVALIDO" -> InvoicingError.InvalidPeriod
            "VALIDACION" -> InvoicingError.Invalid
            "PETICION_DEMASIADO_GRANDE" -> InvoicingError.TooLarge
            "DEMASIADAS_PETICIONES" -> InvoicingError.RateLimited
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
        const val BASE = "/facturacion"
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
