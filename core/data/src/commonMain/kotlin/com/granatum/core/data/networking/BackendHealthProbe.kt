package com.granatum.core.data.networking

import com.granatum.core.data.BuildKonfig
import com.granatum.core.domain.logger.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

/**
 * A one-shot "can we even reach the backend?" check, for development only.
 *
 * Until login exists there is no screen that talks to the server, so there is no way to tell a
 * misconfigured [UrlConstants.BASE_URL_HTTP] from a backend that is simply not running. This probe
 * closes that gap and is what makes the environment configuration verifiable on a device.
 *
 * It deliberately does not go through [BASE_URL_HTTP]: the backend serves Actuator at
 * `/actuator/health`, a sibling of `/api` rather than a child of it, so the API path is trimmed off
 * to get back to the host root.
 *
 * Result goes to the log and nowhere else — this must never change what the user sees, and a dead
 * backend must not stop the app from starting.
 */
private const val LOCAL_ENVIRONMENT = "local"

class BackendHealthProbe(
    private val httpClient: HttpClient,
    private val logger: AppLogger
) {

    suspend fun check() {
        // Development aid only. Guarding here rather than at the call site means no staging or
        // production build can ever be made to emit this, however it gets wired up later.
        if (BuildKonfig.ENVIRONMENT != LOCAL_ENVIRONMENT) return

        val url = healthUrl(UrlConstants.BASE_URL_HTTP)
        runCatching { httpClient.get(url) }
            .onSuccess { response: HttpResponse ->
                logger.info("Backend health check: ${response.status} at $url")
            }
            .onFailure { throwable ->
                logger.warn("Backend unreachable at $url — ${throwable.message}")
            }
    }

    private fun healthUrl(baseUrl: String): String =
        baseUrl.trimEnd('/').removeSuffix("/api") + "/actuator/health"
}
