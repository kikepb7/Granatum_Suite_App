package com.granatum.core.data.networking

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout

/**
 * An HTTP client for the image storage and for loading images, apart from the backend's: it never
 * carries the backend's session token nor its headers to a third party (specs/011, FR-006). It
 * shares the switching engine, so in the Demo environment the demo backend answers it too.
 */
class StorageHttpClient(engine: HttpClientEngine) {
    val client: HttpClient = HttpClient(engine) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000L
            socketTimeoutMillis = 60_000L
        }
        expectSuccess = false
    }
}
