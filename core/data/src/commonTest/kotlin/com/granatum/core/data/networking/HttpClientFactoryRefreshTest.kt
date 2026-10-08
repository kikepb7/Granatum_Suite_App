package com.granatum.core.data.networking

import com.granatum.core.data.testing.AuthTestHarness
import com.granatum.core.data.testing.TestTokens
import com.granatum.core.data.testing.errorJson
import com.granatum.core.data.testing.json
import com.granatum.core.data.testing.tokensJson
import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.model.SignOutReason
import com.granatum.core.domain.auth.model.UserRole
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Research D4: only `TOKEN_ACCESO_EXPIRADO` refreshes, only a definitive rejection ends the
 * session, and concurrent refreshes collapse into one because the refresh token rotates.
 */
class HttpClientFactoryRefreshTest {

    private val expiredAccess = TestTokens.access(marker = "old")
    private val freshAccess = TestTokens.access(marker = "new")

    private val stored = Session(
        accessToken = expiredAccess, refreshToken = "r-old", employeeId = "emp-1",
        role = UserRole.EMPLEADO, mustChangePassword = false, email = "ana@granatum.es"
    )

    private suspend fun AuthTestHarness.signedIn(): AuthTestHarness = apply { storage.set(stored) }

    private suspend fun AuthTestHarness.protectedCall(): HttpResponse = client.get(constructRoute("/fichajes"))

    /** A protected route that accepts only the fresh token. */
    private fun protectedRoute(refresh: suspend io.ktor.client.engine.mock.MockRequestHandleScope.() -> io.ktor.client.request.HttpResponseData) =
        AuthTestHarness { request ->
            when {
                request.url.encodedPath.endsWith("/auth/refresh") -> refresh()
                request.headers["Authorization"] == "Bearer $freshAccess" -> json(HttpStatusCode.OK, "[]")
                else -> json(HttpStatusCode.Unauthorized, errorJson("TOKEN_ACCESO_EXPIRADO"))
            }
        }

    @Test
    fun an_expired_access_token_is_refreshed_and_the_request_repeated() = runTest {
        val h = protectedRoute { json(HttpStatusCode.OK, tokensJson(freshAccess, "r-new")) }.signedIn()

        val response = h.protectedCall()

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, h.requestsTo("/auth/refresh").size)
        val session = h.storage.observeSession().first()!!
        assertEquals("r-new", session.refreshToken)
        assertEquals("ana@granatum.es", session.email)
    }

    @Test
    fun ten_concurrent_requests_refresh_exactly_once() = runTest {
        val h = protectedRoute { json(HttpStatusCode.OK, tokensJson(freshAccess, "r-new")) }.signedIn()

        val statuses = (1..10).map { async { h.protectedCall().status } }.awaitAll()

        assertTrue(statuses.all { it == HttpStatusCode.OK }, "statuses: $statuses")
        assertEquals(1, h.requestsTo("/auth/refresh").size)
        assertEquals("r-new", h.storage.observeSession().first()?.refreshToken)
    }

    @Test
    fun an_invalid_refresh_token_ends_the_session() = runTest {
        val h = protectedRoute { json(HttpStatusCode.Unauthorized, errorJson("TOKEN_RENOVACION_INVALIDO")) }.signedIn()
        h.protectedCall()
        assertNull(h.storage.observeSession().first())
        assertEquals(SignOutReason.SESSION_REJECTED, h.events.lastSignOutReason.value)
    }

    @Test
    fun an_inactive_person_is_signed_out_with_that_reason() = runTest {
        val h = protectedRoute { json(HttpStatusCode.Unauthorized, errorJson("EMPLEADO_INACTIVO")) }.signedIn()
        h.protectedCall()
        assertNull(h.storage.observeSession().first())
        assertEquals(SignOutReason.INACTIVE, h.events.lastSignOutReason.value)
    }

    @Test
    fun no_coverage_during_refresh_keeps_the_session() = runTest {
        val h = protectedRoute { throw kotlinx.io.IOException("offline") }.signedIn()
        runCatching { h.protectedCall() }
        assertEquals(stored, h.storage.observeSession().first())
        assertNull(h.events.lastSignOutReason.value)
    }

    @Test
    fun server_errors_during_refresh_keep_the_session() = runTest {
        listOf(HttpStatusCode.ServiceUnavailable, HttpStatusCode.InternalServerError, HttpStatusCode.TooManyRequests).forEach { status ->
            val h = protectedRoute { json(status, errorJson("SERVICIO_SATURADO")) }.signedIn()
            h.protectedCall()
            assertEquals(stored, h.storage.observeSession().first(), "after $status")
        }
    }

    @Test
    fun not_authenticated_on_a_protected_route_ends_the_session() = runTest {
        val h = AuthTestHarness { json(HttpStatusCode.Unauthorized, errorJson("NO_AUTENTICADO")) }.signedIn()
        h.protectedCall()
        assertNull(h.storage.observeSession().first())
        assertEquals(SignOutReason.SESSION_REJECTED, h.events.lastSignOutReason.value)
        assertTrue(h.requestsTo("/auth/refresh").isEmpty())
    }

    @Test
    fun forbidden_leaves_the_session_alone() = runTest {
        val h = AuthTestHarness { json(HttpStatusCode.Forbidden, errorJson("FORBIDDEN")) }.signedIn()
        h.protectedCall()
        assertEquals(stored, h.storage.observeSession().first())
    }

    @Test
    fun the_401_body_is_still_readable_by_the_caller() = runTest {
        val h = AuthTestHarness { json(HttpStatusCode.Unauthorized, errorJson("NO_AUTENTICADO")) }.signedIn()
        assertTrue(h.protectedCall().bodyAsText().contains("NO_AUTENTICADO"))
    }

    @Test
    fun auth_routes_are_never_logged_and_secrets_are_redacted_elsewhere() = runTest {
        val h = AuthTestHarness { request ->
            if (request.url.encodedPath.endsWith("/auth/login")) json(HttpStatusCode.OK, tokensJson(freshAccess, "r-new"))
            else json(HttpStatusCode.OK, "[]")
        }
        h.repository.login("ana@granatum.es", "Secreto-123!")
        h.protectedCall()

        val log = h.logger.lines.joinToString("\n")
        assertTrue("Secreto-123!" !in log, "password leaked")
        assertTrue(freshAccess !in log, "access token leaked")
        assertTrue("r-new" !in log, "refresh token leaked")
        assertTrue("/auth/login" !in log, "auth route logged")
    }
}
