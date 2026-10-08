package com.granatum.core.data.auth

import com.granatum.core.data.auth.dto.CambioPasswordRequestDto
import com.granatum.core.data.auth.dto.LoginRequestDto
import com.granatum.core.data.auth.dto.LogoutRequestDto
import com.granatum.core.data.auth.dto.RegistroRequestDto
import com.granatum.core.data.auth.dto.ParTokensResponseDto
import com.granatum.core.data.auth.provider.AuthRoutes
import com.granatum.core.data.mappers.normaliseEmail
import com.granatum.core.data.mappers.toSession
import com.granatum.core.data.networking.constructRoute
import com.granatum.core.data.networking.platformSafeCall
import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.OwnerRegistration
import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.logger.AppLogger
import com.granatum.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.firstOrNull

/**
 * The auth routes, reacting to each response as specs/004-login-roles/contracts/auth-api.md
 * says. Login and password change read the error body themselves instead of using the generic
 * status mapping, because what matters is the backend's `code`, not the status.
 */
class KtorAuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
    private val sessionEvents: SessionEvents,
    private val logger: AppLogger
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Session, AuthError> {
        val normalisedEmail = normaliseEmail(email)
        return exchangeForSession(email = normalisedEmail) {
            httpClient.post(constructRoute(AuthRoutes.LOGIN_ROUTE)) {
                // The password goes exactly as typed: trimming it would change someone's password.
                setBody(LoginRequestDto(email = normalisedEmail, password = password))
            }
        }
    }

    override suspend fun registerOwner(registration: OwnerRegistration): Result<Session, AuthError> {
        val email = normaliseEmail(registration.email)
        val sent = exchange {
            httpClient.post(constructRoute(AuthRoutes.REGISTER_ROUTE)) {
                setBody(
                    RegistroRequestDto(
                        email = email,
                        password = registration.password,
                        nombre = registration.name.trim(),
                        documentoIdentidad = registration.identityDocument.trim(),
                        codigoArranque = registration.bootstrapCode.trim()
                    )
                )
            }
        }
        val response = when (sent) {
            is Result.Failure -> return sent
            is Result.Success -> sent.data
        }
        if (!response.status.isSuccess()) return Result.Failure(response.toAuthError())
        // The account exists and is active: sign in as anyone would.
        return login(email = email, password = registration.password)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Session, AuthError> {
        val current = sessionStorage.observeSession().firstOrNull()
            ?: return Result.Failure(AuthError.InvalidSession)
        return exchangeForSession(email = current.email) {
            httpClient.post(constructRoute(AuthRoutes.CHANGE_PASSWORD_ROUTE)) {
                setBody(CambioPasswordRequestDto(passwordActual = currentPassword, passwordNueva = newPassword))
            }
        }
    }

    override suspend fun logout() {
        val refreshToken = sessionStorage.observeSession().firstOrNull()?.refreshToken
        if (refreshToken != null) {
            // Best effort, and short: without coverage this must not keep the person waiting.
            // Idempotent on the server, so a lost response does no harm either.
            exchange {
                httpClient.post(constructRoute(AuthRoutes.LOGOUT_ROUTE)) {
                    timeout { requestTimeoutMillis = LOGOUT_TIMEOUT_MS }
                    setBody(LogoutRequestDto(refreshToken = refreshToken))
                }
            }
        }
        sessionEvents.clearReason()
        sessionStorage.set(null)
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
    }

    private suspend fun exchangeForSession(
        email: String,
        request: suspend () -> HttpResponse
    ): Result<Session, AuthError> {
        val response = when (val sent = exchange(request)) {
            is Result.Failure -> return sent
            is Result.Success -> sent.data
        }
        if (!response.status.isSuccess()) return Result.Failure(response.toAuthError())

        val tokens = runCatching { response.body<ParTokensResponseDto>() }.getOrElse {
            logger.warn("Auth response did not match the contract: ${it::class.simpleName}")
            return Result.Failure(AuthError.Unknown)
        }
        val session = when (val mapped = tokens.toSession(email = email, logger = logger)) {
            is Result.Failure -> return mapped
            is Result.Success -> mapped.data
        }
        sessionEvents.clearReason()
        sessionStorage.set(session)
        // The plugin caches tokens; make it reload them from the session just stored.
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
        return Result.Success(session)
    }

    /** Sends the request; transport failures are classified per platform by `platformSafeCall`. */
    private suspend fun exchange(request: suspend () -> HttpResponse): Result<HttpResponse, AuthError> {
        var response: HttpResponse? = null
        val sent = platformSafeCall(execute = request) { received ->
            response = received
            Result.Success(Unit)
        }
        return when (sent) {
            is Result.Failure -> Result.Failure(sent.error.toAuthError())
            is Result.Success -> Result.Success(response!!)
        }
    }

    private companion object {
        const val LOGOUT_TIMEOUT_MS = 5_000L
    }
}
