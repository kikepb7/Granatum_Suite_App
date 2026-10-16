package com.granatum.core.data.networking

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout

/**
 * The client images are loaded with: the backend hands out temporary signed URLs to its storage
 * (backend 010, research D-005), which need neither the session token nor the API key, so neither
 * is sent to a third party. It shares the switching engine, so in the Demo environment the demo
 * backend serves its images too.
 */
class ImageHttpClient(engine: HttpClientEngine) {
    val client: HttpClient = HttpClient(engine) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000L
            socketTimeoutMillis = 60_000L
        }
        expectSuccess = false
    }
}
