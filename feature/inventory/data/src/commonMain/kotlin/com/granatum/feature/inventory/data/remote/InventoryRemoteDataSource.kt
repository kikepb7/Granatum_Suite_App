package com.granatum.feature.inventory.data.remote

import com.granatum.core.data.auth.errorBody
import com.granatum.core.data.networking.constructRoute
import com.granatum.core.data.networking.platformSafeCall
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.data.dto.CategoryDto
import com.granatum.feature.inventory.data.dto.CategoryRequestDto
import com.granatum.feature.inventory.data.dto.CreateMaterialRequestDto
import com.granatum.feature.inventory.data.dto.MaterialHistoryDto
import com.granatum.feature.inventory.data.dto.MaterialDto
import com.granatum.feature.inventory.data.dto.UpdateQuantityRequestDto
import com.granatum.feature.inventory.data.dto.UpdateMaterialRequestDto
import com.granatum.feature.inventory.domain.model.InventoryError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/** The inventory routes of docs/openapi.json (backend 010), errors as its contracts/README.md lists them. */
class InventoryRemoteDataSource(private val http: HttpClient) {

    suspend fun materials() = call<List<MaterialDto>> { http.get(constructRoute("/materials")) }
    suspend fun material(id: String) = call<MaterialDto> { http.get(constructRoute("/materials/$id")) }
    suspend fun history(id: String) = call<List<MaterialHistoryDto>> { http.get(constructRoute("/materials/$id/history")) }
    suspend fun create(body: CreateMaterialRequestDto) = call<MaterialDto> { http.post(constructRoute("/materials")) { setBody(body) } }
    suspend fun update(id: String, body: UpdateMaterialRequestDto) = call<MaterialDto> { http.put(constructRoute("/materials/$id")) { setBody(body) } }
    suspend fun adjust(id: String, body: UpdateQuantityRequestDto) = call<MaterialDto> { http.patch(constructRoute("/materials/$id/quantity")) { setBody(body) } }
    suspend fun delete(id: String) = call<Unit> { http.delete(constructRoute("/materials/$id")) }

    /** One `image` part; the server decides the type by its bytes, not by the name or header. */
    suspend fun uploadImage(id: String, bytes: ByteArray, fileName: String, mimeType: String) = call<MaterialDto> {
        http.put(constructRoute("/materials/$id/image")) {
            timeout {
                requestTimeoutMillis = TRANSFER_TIMEOUT_MILLIS
                socketTimeoutMillis = TRANSFER_TIMEOUT_MILLIS
            }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "image",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"${fileName.replace(Regex("[\"\\\\\r\n]"), "_")}\"")
                            }
                        )
                    }
                )
            )
        }
    }

    suspend fun removeImage(id: String) = call<MaterialDto> { http.delete(constructRoute("/materials/$id/image")) }

    suspend fun categories() = call<List<CategoryDto>> { http.get(constructRoute("/categories")) }
    suspend fun createCategory(body: CategoryRequestDto) = call<CategoryDto> { http.post(constructRoute("/categories")) { setBody(body) } }
    suspend fun updateCategory(id: String, body: CategoryRequestDto) = call<CategoryDto> { http.put(constructRoute("/categories/$id")) { setBody(body) } }
    suspend fun deleteCategory(id: String) = call<Unit> { http.delete(constructRoute("/categories/$id")) }

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
        "MATERIAL_NOT_FOUND" -> InventoryError.ProductNotFound
        "MATERIAL_IN_USE" -> InventoryError.ProductInUse
        "CATEGORY_NOT_FOUND" -> InventoryError.CategoryNotFound
        "INVALID_OPERATION" -> InventoryError.QuantityOutOfRange
        "VALIDATION" -> InventoryError.Invalid
        "INVALID_IMAGE" -> InventoryError.ImageInvalid
        "IMAGE_TOO_LARGE" -> InventoryError.ImageTooLarge
        "IMAGE_STORAGE_UNAVAILABLE" -> InventoryError.StorageUnavailable
        else -> when (status) {
            HttpStatusCode.Forbidden -> InventoryError.Forbidden
            // A proxy may cut a large upload before the server sees it.
            HttpStatusCode.PayloadTooLarge -> InventoryError.ImageTooLarge
            else -> InventoryError.Unknown
        }
    }

    private companion object {
        /** An image of up to 10 MB over a weak mobile connection. */
        const val TRANSFER_TIMEOUT_MILLIS = 120_000L
    }
}
