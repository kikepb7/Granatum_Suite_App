package com.granatum.core.data.auth

import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.validation.PasswordRequirement
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthErrorMapperTest {

    private suspend fun response(
        status: HttpStatusCode,
        body: String,
        vararg headers: Pair<String, String>
    ): HttpResponse {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(*(headers.map { it.first to listOf(it.second) } +
                    (HttpHeaders.ContentType to listOf("application/json"))).toTypedArray())
            )
        }
        return HttpClient(engine).get("http://test/")
    }

    private fun error(code: String, extra: String = "") = """{"code":"$code","message":"m"$extra}"""

    @Test
    fun the_four_rejected_sign_in_cases_share_one_error() = runTest {
        assertEquals(AuthError.InvalidCredentials, response(HttpStatusCode.Unauthorized, error("CREDENCIALES_INVALIDAS")).toAuthError())
    }

    @Test
    fun rate_limit_and_overload_carry_retry_after_when_present() = runTest {
        assertEquals(
            AuthError.TooManyAttempts(30),
            response(HttpStatusCode.TooManyRequests, error("DEMASIADAS_PETICIONES"), HttpHeaders.RetryAfter to "30").toAuthError()
        )
        assertEquals(
            AuthError.ServiceBusy(5),
            response(HttpStatusCode.ServiceUnavailable, error("SERVICIO_SATURADO"), HttpHeaders.RetryAfter to " 5 ").toAuthError()
        )
    }

    @Test
    fun a_missing_or_non_numeric_retry_after_gives_no_figure() = runTest {
        assertEquals(AuthError.TooManyAttempts(null), response(HttpStatusCode.TooManyRequests, error("DEMASIADAS_PETICIONES")).toAuthError())
        assertEquals(
            AuthError.TooManyAttempts(null),
            response(HttpStatusCode.TooManyRequests, error("DEMASIADAS_PETICIONES"), HttpHeaders.RetryAfter to "Wed, 21 Oct 2026 07:28:00 GMT").toAuthError()
        )
    }

    @Test
    fun weak_password_keeps_the_requirements_it_knows() = runTest {
        val body = error("PASSWORD_DEBIL", ""","requisitos":["FALTA_SIMBOLO","LONGITUD_MINIMA","ALGO_NUEVO"]""")
        assertEquals(
            AuthError.WeakPassword(setOf(PasswordRequirement.FALTA_SIMBOLO, PasswordRequirement.LONGITUD_MINIMA)),
            response(HttpStatusCode.UnprocessableEntity, body).toAuthError()
        )
    }

    @Test
    fun validation_and_unknown_codes() = runTest {
        assertEquals(AuthError.Validation, response(HttpStatusCode.BadRequest, error("VALIDACION")).toAuthError())
        assertEquals(AuthError.Unknown, response(HttpStatusCode.Conflict, error("OTRA_COSA")).toAuthError())
        assertEquals(AuthError.Unknown, response(HttpStatusCode.InternalServerError, "<html>oops</html>").toAuthError())
    }

    @Test
    fun transport_failures() {
        assertEquals(AuthError.NoInternet, DataError.Remote.NO_INTERNET.toAuthError())
        assertEquals(AuthError.Timeout, DataError.Remote.REQUEST_TIMEOUT.toAuthError())
        assertEquals(AuthError.Unknown, DataError.Remote.SERIALIZATION.toAuthError())
    }
}
