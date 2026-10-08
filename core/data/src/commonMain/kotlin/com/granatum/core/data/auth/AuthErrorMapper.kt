package com.granatum.core.data.auth

import com.granatum.core.data.auth.dto.ErrorBodyDto
import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.validation.PasswordRequirement
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * The stable `code` of a backend error response, or null if the body is not `{code, message}`.
 * Never throws: it is also called from inside the HTTP client's auth plugin.
 */
suspend fun HttpResponse.errorBody(): ErrorBodyDto? =
    runCatching { errorJson.decodeFromString<ErrorBodyDto>(bodyAsText()) }.getOrNull()

/**
 * Maps a non-2xx response of an auth route by its `code` (docs/api-contract.md). The codes come
 * from the backend's specs/002-auth/contracts/README.md at the pinned commit; the status alone
 * is not enough, because several codes share a 401.
 */
suspend fun HttpResponse.toAuthError(): AuthError {
    val body = errorBody()
    return when (body?.code) {
        "CREDENCIALES_INVALIDAS" -> AuthError.InvalidCredentials
        "DEMASIADAS_PETICIONES" -> AuthError.TooManyAttempts(retryAfterSeconds())
        "SERVICIO_SATURADO" -> AuthError.ServiceBusy(retryAfterSeconds())
        "PASSWORD_DEBIL" -> AuthError.WeakPassword(
            body.requisitos.orEmpty().mapNotNull(PasswordRequirement::fromBackend).toSet()
        )
        "VALIDACION" -> AuthError.Validation
        else -> AuthError.Unknown
    }
}

/** `Retry-After` in seconds; the HTTP-date form is not used by this backend and is ignored. */
private fun HttpResponse.retryAfterSeconds(): Long? =
    headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull()?.takeIf { it >= 0 }

/** Transport failures, already classified per platform by `platformSafeCall`. */
fun DataError.Remote.toAuthError(): AuthError = when (this) {
    DataError.Remote.NO_INTERNET -> AuthError.NoInternet
    DataError.Remote.REQUEST_TIMEOUT -> AuthError.Timeout
    else -> AuthError.Unknown
}
