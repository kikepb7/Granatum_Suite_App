package com.granatum.feature.inventory.data.storage

import com.granatum.core.data.environment.ActiveEnvironment
import com.granatum.core.data.environment.StorageConfig
import com.granatum.core.data.networking.StorageHttpClient
import com.granatum.core.data.networking.platformSafeCall
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.domain.model.InventoryError
import com.granatum.feature.inventory.domain.repository.ProductImageStorage
import com.granatum.feature.inventory.domain.validation.InventoryValidation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Uploads product images straight to the environment's object storage (Supabase Storage's REST
 * API) and returns their public address, which the product keeps as its image (specs/011,
 * research D3). Uses its own HTTP client: the backend's session token never reaches the storage.
 */
class SupabaseProductImageStorage(
    private val http: StorageHttpClient,
    private val config: () -> StorageConfig? = { ActiveEnvironment.storage }
) : ProductImageStorage {

    override val isAvailable: Boolean get() = config() != null

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun upload(bytes: ByteArray, contentType: String): Result<String, InventoryError> {
        val storage = config() ?: return Result.Failure(InventoryError.StorageUnavailable)
        if (bytes.size > InventoryValidation.IMAGE_MAX_BYTES) return Result.Failure(InventoryError.ImageTooLarge)
        val path = "productos/${Uuid.random()}.${extensionFor(contentType)}"
        var response: HttpResponse? = null
        val sent = platformSafeCall(
            execute = {
                http.client.post(storage.uploadUrl(path)) {
                    header(HttpHeaders.Authorization, "Bearer ${storage.key}")
                    header("apikey", storage.key)
                    header("x-upsert", "false")
                    contentType(ContentType.parse(contentType))
                    setBody(bytes)
                }
            }
        ) { received ->
            response = received
            Result.Success(Unit)
        }
        return when (sent) {
            is Result.Failure -> Result.Failure(
                if (sent.error == DataError.Remote.NO_INTERNET || sent.error == DataError.Remote.REQUEST_TIMEOUT) InventoryError.NoInternet
                else InventoryError.ImageUploadFailed
            )
            is Result.Success -> when {
                response?.status?.isSuccess() == true -> Result.Success(storage.publicUrl(path))
                response?.status?.value == 413 -> Result.Failure(InventoryError.ImageTooLarge)
                else -> Result.Failure(InventoryError.ImageUploadFailed)
            }
        }
    }

    private fun extensionFor(contentType: String) = when (contentType.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
}
