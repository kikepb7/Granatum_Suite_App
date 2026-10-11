package com.granatum.core.data.environment

import com.granatum.core.domain.environment.AppEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.utils.io.InternalAPI

/**
 * The platform's engine, except in the Demo environment, whose requests go to the in-memory demo
 * backend instead (research D5). Switching needs no new HTTP client: every repository keeps the
 * one it has.
 */
@OptIn(InternalAPI::class)
class SwitchingEngine(
    private val real: HttpClientEngine,
    private val demo: HttpClientEngine?,
    private val environment: () -> AppEnvironment = { ActiveEnvironment.current }
) : HttpClientEngine by real {

    override suspend fun execute(data: HttpRequestData): HttpResponseData =
        if (demo != null && environment() == AppEnvironment.DEMO) demo.execute(data) else real.execute(data)

    /** The interface's own install, so the client's pipeline calls this engine's [execute]. */
    override fun install(client: HttpClient) = super<HttpClientEngine>.install(client)

    override fun close() {
        real.close()
        demo?.close()
    }
}
