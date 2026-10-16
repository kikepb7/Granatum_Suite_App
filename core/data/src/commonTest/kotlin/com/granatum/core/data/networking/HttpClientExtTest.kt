package com.granatum.core.data.networking

import com.granatum.core.data.networking.UrlConstants.BASE_URL_HTTP
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@Serializable
private data class Ping(val name: String)

class HttpClientExtTest {

    private var status = HttpStatusCode.OK
    private var body = """{"name":"ok"}"""
    private var contentType = "application/json"
    private val engine = MockEngine {
        respond(body, status, headersOf(HttpHeaders.ContentType, contentType))
    }
    private val client = HttpClient(engine) { install(ContentNegotiation) { json() } }

    private fun failure(result: Result<*, DataError.Remote>) = (result as Result.Failure).error

    @Test
    fun constructRoute_handles_all_three_shapes() {
        assertEquals("$BASE_URL_HTTP/a", constructRoute("/a"))
        assertEquals("$BASE_URL_HTTP/a", constructRoute("a"))
        assertEquals("$BASE_URL_HTTP/a/b", constructRoute("$BASE_URL_HTTP/a/b"))
    }

    @Test
    fun every_2xx_is_decoded_into_the_body() = runTest {
        for (code in listOf(200, 201, 299)) {
            status = HttpStatusCode.fromValue(code)
            assertEquals(Result.Success(Ping("ok")), client.get<Ping>("/ping"))
        }
    }

    @Test
    fun a_body_that_cannot_be_transformed_is_a_serialization_error() = runTest {
        contentType = "text/plain"
        assertEquals(DataError.Remote.SERIALIZATION, failure(client.get<Ping>("/ping")))
    }

    @Test
    fun error_statuses_map_to_their_remote_error() = runTest {
        val expected = mapOf(
            400 to DataError.Remote.BAD_REQUEST,
            401 to DataError.Remote.UNAUTHORIZED,
            403 to DataError.Remote.FORBIDDEN,
            404 to DataError.Remote.NOT_FOUND,
            408 to DataError.Remote.REQUEST_TIMEOUT,
            409 to DataError.Remote.CONFLICT,
            413 to DataError.Remote.PAYLOAD_TOO_LARGE,
            429 to DataError.Remote.TOO_MANY_REQUESTS,
            500 to DataError.Remote.SERVER_ERROR,
            503 to DataError.Remote.SERVICE_UNAVAILABLE,
            418 to DataError.Remote.UNKNOWN,
            502 to DataError.Remote.UNKNOWN
        )
        for ((code, error) in expected) {
            status = HttpStatusCode.fromValue(code)
            assertEquals(error, failure(client.get<Ping>("/ping")), "status $code")
        }
    }

    @Test
    fun get_sends_query_params_to_the_built_route() = runTest {
        client.get<Ping>("items", mapOf("page" to 2, "q" to "a b")) { headers.append("X-Test", "1") }

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Get, request.method)
        assertTrue(request.url.encodedPath.endsWith("/items"))
        assertEquals("2", request.url.parameters["page"])
        assertEquals("a b", request.url.parameters["q"])
        assertEquals("1", request.headers["X-Test"])
    }

    @Test
    fun post_sends_the_body_with_its_params() = runTest {
        assertEquals(Result.Success(Ping("ok")), client.post<Ping, Ping>("/p", Ping("in"), mapOf("k" to "v")) { contentType(ContentType.Application.Json) })
        val request = engine.requestHistory.last()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("v", request.url.parameters["k"])
    }

    @Test
    fun put_sends_the_body_with_its_params() = runTest {
        assertEquals(Result.Success(Ping("ok")), client.put<Ping, Ping>("/p", mapOf("k" to "v"), Ping("in")) { contentType(ContentType.Application.Json) })
        val request = engine.requestHistory.last()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("v", request.url.parameters["k"])
    }

    @Test
    fun delete_uses_its_method_and_params() = runTest {
        assertEquals(Result.Success(Ping("ok")), client.delete<Ping>("/p", mapOf("k" to "v")))
        val request = engine.requestHistory.last()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("v", request.url.parameters["k"])
    }

    @Test
    fun postMultipart_posts_the_form() = runTest {
        val content = MultiPartFormDataContent(formData { append("field", "value") })
        val result = client.postMultipart<Ping>("/upload", content)

        assertEquals(Result.Success(Ping("ok")), result)
        assertEquals(HttpMethod.Post, engine.requestHistory.single().method)
    }

    @Test
    fun platform_failures_become_remote_errors() = runTest {
        suspend fun mapped(throwable: Throwable): DataError.Remote = failure(
            platformSafeCall<Unit>(execute = { throw throwable }, handleResponse = { Result.Success(Unit) })
        )

        assertEquals(DataError.Remote.NO_INTERNET, mapped(UnresolvedAddressException()))
        assertEquals(DataError.Remote.SERIALIZATION, mapped(SerializationException("bad")))
        assertEquals(DataError.Remote.UNKNOWN, mapped(IllegalStateException("?")))
    }

    @Test
    fun a_response_is_handed_to_the_handler() = runTest {
        val result = platformSafeCall<Int>(
            execute = { client.get("$BASE_URL_HTTP/x") },
            handleResponse = { r: HttpResponse -> Result.Success(r.status.value) }
        )
        assertIs<Result.Success<Int>>(result)
        assertEquals(200, result.data)
    }
}
