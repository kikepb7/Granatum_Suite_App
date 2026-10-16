package com.granatum.feature.inventory.data

import com.granatum.core.data.environment.StorageConfig
import com.granatum.core.data.networking.StorageHttpClient
import com.granatum.core.domain.util.Result
import com.granatum.feature.inventory.data.storage.SupabaseProductImageStorage
import com.granatum.feature.inventory.domain.model.InventoryError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** specs/011-inventario-productos, research D3. */
class ProductImageStorageTest {
    private val config = StorageConfig(baseUrl = "https://storage.test", key = "anon-key", bucket = "inventario")
    private val seen = mutableListOf<HttpRequestData>()

    private fun storage(status: HttpStatusCode = HttpStatusCode.OK, config: StorageConfig? = this.config, fail: Boolean = false) =
        SupabaseProductImageStorage(
            StorageHttpClient(MockEngine { request ->
                seen += request
                if (fail) throw kotlinx.io.IOException("offline")
                respond("""{"Key":"x"}""", status)
            }),
            config = { config }
        )

    @Test
    fun uploads_to_the_bucket_with_the_storage_key_and_returns_the_public_address() = runTest {
        val result = storage().upload(byteArrayOf(1, 2, 3), "image/png")
        val request = seen.single()
        assertEquals(HttpMethod.Post, request.method)
        val path = request.url.encodedPath
        assertTrue(path.startsWith("/storage/v1/object/inventario/productos/") && path.endsWith(".png"), path)
        assertEquals("Bearer anon-key", request.headers["Authorization"])
        assertEquals("anon-key", request.headers["apikey"])
        assertEquals("false", request.headers["x-upsert"])
        val url = (result as Result.Success).data
        assertEquals("https://storage.test/storage/v1/object/public/inventario${path.removePrefix("/storage/v1/object/inventario")}", url)
    }

    @Test
    fun without_a_configured_storage_nothing_is_sent() = runTest {
        val storage = storage(config = null)
        assertFalse(storage.isAvailable)
        assertEquals(Result.Failure(InventoryError.StorageUnavailable), storage.upload(byteArrayOf(1), "image/jpeg"))
        assertTrue(seen.isEmpty())
    }

    @Test
    fun failures_are_told_apart() = runTest {
        assertEquals(Result.Failure(InventoryError.ImageTooLarge), storage(HttpStatusCode.PayloadTooLarge).upload(byteArrayOf(1), "image/jpeg"))
        assertEquals(Result.Failure(InventoryError.ImageUploadFailed), storage(HttpStatusCode.Forbidden).upload(byteArrayOf(1), "image/jpeg"))
        assertEquals(Result.Failure(InventoryError.NoInternet), storage(fail = true).upload(byteArrayOf(1), "image/jpeg"))
    }

    @Test
    fun an_image_over_the_limit_is_refused_before_sending() = runTest {
        val result = storage().upload(ByteArray(10 * 1024 * 1024 + 1), "image/jpeg")
        assertEquals(Result.Failure(InventoryError.ImageTooLarge), result)
        assertTrue(seen.isEmpty())
    }
}
