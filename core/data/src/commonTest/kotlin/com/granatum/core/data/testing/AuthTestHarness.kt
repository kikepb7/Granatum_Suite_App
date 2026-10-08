package com.granatum.core.data.testing

import com.granatum.core.data.auth.KtorAuthRepositoryImpl
import com.granatum.core.data.auth.SessionEvents
import com.granatum.core.data.auth.storage.SecureSessionStorage
import com.granatum.core.data.networking.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/** The real client, storage and repository wired over a [MockEngine]. */
class AuthTestHarness(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
) {
    val logger = RecordingLogger()
    val store = FakeSecureStore()
    val storage = SecureSessionStorage(store, logger)
    val events = SessionEvents(storage)
    val engine = MockEngine(handler)
    val client = HttpClientFactory(logger, storage, events).create(engine)
    val repository = KtorAuthRepositoryImpl(client, storage, events, logger)

    fun requestsTo(pathSuffix: String) = engine.requestHistory.filter { it.url.encodedPath.endsWith(pathSuffix) }
}

fun MockRequestHandleScope.json(status: HttpStatusCode, body: String, vararg extra: Pair<String, String>) = respond(
    content = body,
    status = status,
    headers = headersOf(*((extra.map { it.first to listOf(it.second) }) + (HttpHeaders.ContentType to listOf("application/json"))).toTypedArray())
)

fun tokensJson(access: String, refresh: String, mustChange: Boolean = false) =
    """{"accessToken":"$access","refreshToken":"$refresh","expiresIn":900,"requiereCambioPassword":$mustChange}"""

fun errorJson(code: String) = """{"code":"$code","message":"m"}"""
