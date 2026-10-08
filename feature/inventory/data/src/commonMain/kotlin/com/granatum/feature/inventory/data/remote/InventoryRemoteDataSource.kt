package com.granatum.feature.inventory.data.remote

import com.granatum.core.data.auth.errorBody
import com.granatum.core.data.networking.constructRoute
import com.granatum.core.data.networking.platformSafeCall
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.data.dto.CategoriaDto
import com.granatum.feature.inventory.data.dto.CategoriaRequestDto
import com.granatum.feature.inventory.data.dto.CreateMaterialRequestDto
import com.granatum.feature.inventory.data.dto.HistorialMaterialDto
import com.granatum.feature.inventory.data.dto.MaterialDto
import com.granatum.feature.inventory.data.dto.UpdateCantidadRequestDto
import com.granatum.feature.inventory.data.dto.UpdateMaterialRequestDto
import com.granatum.feature.inventory.domain.model.InventoryError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/** The inventory routes of docs/openapi.json (backend d1857ad), errors as research D5 maps them. */
class InventoryRemoteDataSource(private val http: HttpClient) {

    suspend fun materials() = call<List<MaterialDto>> { http.get(constructRoute("/materiales")) }
    suspend fun material(id: String) = call<MaterialDto> { http.get(constructRoute("/materiales/$id")) }
    suspend fun history(id: String) = call<List<HistorialMaterialDto>> { http.get(constructRoute("/materiales/$id/historial")) }
    suspend fun create(body: CreateMaterialRequestDto) = call<MaterialDto> { http.post(constructRoute("/materiales")) { setBody(body) } }
    suspend fun update(id: String, body: UpdateMaterialRequestDto) = call<MaterialDto> { http.put(constructRoute("/materiales/$id")) { setBody(body) } }
    suspend fun adjust(id: String, body: UpdateCantidadRequestDto) = call<MaterialDto> { http.patch(constructRoute("/materiales/$id/cantidad")) { setBody(body) } }
    suspend fun delete(id: String) = call<Unit> { http.delete(constructRoute("/materiales/$id")) }

    suspend fun categories() = call<List<CategoriaDto>> { http.get(constructRoute("/categorias")) }
    suspend fun createCategory(body: CategoriaRequestDto) = call<CategoriaDto> { http.post(constructRoute("/categorias")) { setBody(body) } }
    suspend fun updateCategory(id: String, body: CategoriaRequestDto) = call<CategoriaDto> { http.put(constructRoute("/categorias/$id")) { setBody(body) } }
    suspend fun deleteCategory(id: String) = call<Unit> { http.delete(constructRoute("/categorias/$id")) }

    private suspend inline fun <reified T> call(noinline request: suspend () -> HttpResponse): Result<T, InventoryError> {
        var response: HttpResponse? = null
        val sent = platformSafeCall(execute = request) { received ->
            response = received
            Result.Success(Unit)
        }
        val ok = when (sent) {
            is Result.Failure -> return Result.Failure(
                if (sent.error == DataError.Remote.NO_INTERNET || sent.error == DataError.Remote.REQUEST_TIMEOUT) InventoryError.NoInternet
                else InventoryError.Unknown
            )
            is Result.Success -> response!!
        }
        if (!ok.status.isSuccess()) return Result.Failure(ok.toInventoryError())
        if (T::class == Unit::class) {
            @Suppress("UNCHECKED_CAST")
            return Result.Success(Unit as T)
        }
        return runCatching { Result.Success(ok.body<T>()) }.getOrElse { Result.Failure(InventoryError.Unknown) }
    }

    private suspend fun HttpResponse.toInventoryError(): InventoryError = when (errorBody()?.code) {
        "MATERIAL_NOT_FOUND" -> InventoryError.MaterialNotFound
        "CATEGORIA_NOT_FOUND" -> InventoryError.CategoryNotFound
        "INVALID_OPERATION" -> InventoryError.QuantityOutOfRange
        "VALIDACION" -> InventoryError.Invalid
        else -> if (status == HttpStatusCode.Forbidden) InventoryError.Forbidden else InventoryError.Unknown
    }
}
