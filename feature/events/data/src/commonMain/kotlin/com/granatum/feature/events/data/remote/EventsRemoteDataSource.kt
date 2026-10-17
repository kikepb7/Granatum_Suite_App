package com.granatum.feature.events.data.remote

import com.granatum.core.data.networking.constructRoute
import com.granatum.core.data.networking.platformSafeCall
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result
import com.granatum.feature.events.data.dto.CantidadMaterialEventoRequestDto
import com.granatum.feature.events.data.dto.EventoDto
import com.granatum.feature.events.data.dto.EventoRequestDto
import com.granatum.feature.events.data.dto.EventsErrorBodyDto
import com.granatum.feature.events.data.dto.MaterialDisponibleDto
import com.granatum.feature.events.domain.model.EventsError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/** The event routes of docs/openapi.json (backend dc2f907), errors as specs/011-events/contracts lists them. */
internal class EventsRemoteDataSource(private val http: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun events() = call<List<EventoDto>> { http.get(constructRoute(BASE)) }
    suspend fun event(id: String) = call<EventoDto> { http.get(constructRoute("$BASE/$id")) }
    suspend fun create(body: EventoRequestDto) = call<EventoDto> { http.post(constructRoute(BASE)) { setBody(body) } }
    suspend fun update(id: String, body: EventoRequestDto) = call<EventoDto> { http.put(constructRoute("$BASE/$id")) { setBody(body) } }
    suspend fun delete(id: String) = call<Unit> { http.delete(constructRoute("$BASE/$id")) }
    suspend fun finish(id: String) = call<EventoDto> { http.post(constructRoute("$BASE/$id/finalizar")) }

    suspend fun setMaterial(id: String, materialId: String, quantity: Int) = call<EventoDto> {
        http.put(constructRoute("$BASE/$id/materiales/$materialId")) { setBody(CantidadMaterialEventoRequestDto(quantity)) }
    }

    suspend fun removeMaterial(id: String, materialId: String) = call<EventoDto> {
        http.delete(constructRoute("$BASE/$id/materiales/$materialId"))
    }

    suspend fun availableMaterials() = call<List<MaterialDisponibleDto>> { http.get(constructRoute("$BASE/materiales")) }

    private suspend inline fun <reified T> call(noinline request: suspend () -> HttpResponse): Result<T, EventsError> {
        var response: HttpResponse? = null
        val sent = platformSafeCall(execute = request) { received ->
            response = received
            Result.Success(Unit)
        }
        val ok = when (sent) {
            is Result.Failure -> return Result.Failure(
                if (sent.error == DataError.Remote.NO_INTERNET || sent.error == DataError.Remote.REQUEST_TIMEOUT) EventsError.NoInternet
                else EventsError.Unknown
            )
            is Result.Success -> response!!
        }
        if (!ok.status.isSuccess()) return Result.Failure(ok.toEventsError())
        if (T::class == Unit::class) {
            @Suppress("UNCHECKED_CAST")
            return Result.Success(Unit as T)
        }
        return runCatching { Result.Success(ok.body<T>()) }.getOrElse { Result.Failure(EventsError.Unknown) }
    }

    private suspend fun HttpResponse.toEventsError(): EventsError {
        val body = runCatching { json.decodeFromString<EventsErrorBodyDto>(bodyAsText()) }.getOrNull()
        return when (body?.code) {
            "EVENTO_NOT_FOUND" -> EventsError.EventNotFound
            "MATERIAL_NOT_FOUND" -> EventsError.MaterialNotFound
            "STOCK_INSUFICIENTE" -> EventsError.InsufficientStock(available = body.disponible ?: 0)
            "EVENTO_FINALIZADO" -> EventsError.EventFinished
            "FECHA_PASADA" -> EventsError.DateInPast
            "VALIDACION" -> EventsError.Invalid
            else -> if (status == HttpStatusCode.Forbidden) EventsError.Forbidden else EventsError.Unknown
        }
    }

    private companion object {
        const val BASE = "/eventos"
    }
}
