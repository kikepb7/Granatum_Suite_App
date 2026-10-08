package com.granatum.core.data.networking

import com.granatum.core.data.BuildKonfig
import com.granatum.core.data.auth.SessionEvents
import com.granatum.core.data.auth.dto.ParTokensResponseDto
import com.granatum.core.data.auth.dto.RefreshRequestDto
import com.granatum.core.data.auth.errorBody
import com.granatum.core.data.auth.provider.AuthRoutes
import com.granatum.core.data.mappers.toSession
import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.model.SignOutReason
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.logger.AppLogger
import com.granatum.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.RefreshTokensParams
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

/**
 * The shared HTTP client. Besides the usual plumbing it owns the session's continuity, the part
 * that makes this app usable offline (specs/004-login-roles, research D4):
 *
 * - Only `401 TOKEN_ACCESO_EXPIRADO` triggers a refresh. Every other 401 means something else:
 *   `CREDENCIALES_INVALIDAS` on a password change is a wrong password, not an expired session.
 * - Concurrent refreshes collapse into one inside Ktor's bearer provider (`AuthTokenHolder`
 *   serialises them), which matters because the refresh token rotates on every use: spending it
 *   twice would end the session.
 * - The session ends only when the server rejects it for good. No coverage, a timeout or a 5xx
 *   keep it, so a person with an expired access token can still open the app and clock in.
 */
class HttpClientFactory(
    private val appLogger: AppLogger,
    private val sessionStorage: SessionStorage,
    private val sessionEvents: SessionEvents
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine = engine) {
            install(ContentNegotiation) {
                json(json = json)
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 20_000L
                requestTimeoutMillis = 20_000L
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) = appLogger.debug(message = message)
                }
                level = LogLevel.ALL
                // Passwords and tokens travel in the bodies of the auth routes, so those calls
                // are not logged at all, request or response (FR-030). Neither are invoicing
                // calls: third parties' tax data and whole documents (specs/008-facturacion).
                filter { request ->
                    val path = request.url.encodedPath
                    !path.contains("/auth/") && !path.contains("/facturacion/")
                }
                sanitizeHeader { name -> name == HttpHeaders.Authorization || name == API_KEY_HEADER }
            }
            install(WebSockets) {
                pingIntervalMillis = 20_000L
            }
            defaultRequest {
                header(API_KEY_HEADER, BuildKonfig.API_KEY)
                contentType(ContentType.Application.Json)
            }
            HttpResponseValidator {
                validateResponse { response ->
                    // A protected route saying the token is not valid at all: no refresh can fix
                    // that. Public auth routes answer with their own codes and are left alone.
                    if (response.status == HttpStatusCode.Unauthorized &&
                        !response.request.url.isPublicAuthRoute() &&
                        response.errorBody()?.code == CODE_NOT_AUTHENTICATED
                    ) {
                        sessionEvents.forceSignOut(SignOutReason.SESSION_REJECTED)
                    }
                }
            }
            install(Auth) {
                reAuthorizeOnResponse { response ->
                    response.status == HttpStatusCode.Unauthorized &&
                        response.errorBody()?.code == CODE_ACCESS_EXPIRED
                }
                bearer {
                    sendWithoutRequest { request -> !request.url.build().isPublicAuthRoute() }
                    loadTokens {
                        sessionStorage.observeSession().firstOrNull()?.toBearerTokens()
                    }
                    refreshTokens { refresh() }
                }
            }
        }
    }

    private suspend fun RefreshTokensParams.refresh(): BearerTokens? {
        val current = sessionStorage.observeSession().firstOrNull() ?: return null

        val response = try {
            client.post(constructRoute(AuthRoutes.REFRESH_ROUTE)) {
                markAsRefreshTokenRequest()
                setBody(RefreshRequestDto(refreshToken = current.refreshToken))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No coverage or a timeout. Keep the session; the next request tries again.
            appLogger.info("Token refresh deferred: ${e::class.simpleName}")
            return null
        }

        return when {
            response.status.isSuccess() -> storeRefreshed(response, current)
            response.status == HttpStatusCode.Unauthorized -> {
                when (response.errorBody()?.code) {
                    CODE_REFRESH_INVALID -> sessionEvents.forceSignOut(SignOutReason.SESSION_REJECTED)
                    CODE_EMPLOYEE_INACTIVE -> sessionEvents.forceSignOut(SignOutReason.INACTIVE)
                    // An undocumented 401 is not treated as a rejection: guessing would sign
                    // people out on something the contract never promised (principle VI).
                    else -> appLogger.warn("Token refresh refused without a known code; session kept")
                }
                null
            }
            else -> {
                // 429 or 5xx: temporary by definition.
                appLogger.info("Token refresh deferred: HTTP ${response.status.value}")
                null
            }
        }
    }

    private suspend fun storeRefreshed(response: HttpResponse, current: Session): BearerTokens? {
        val tokens = runCatching { response.body<ParTokensResponseDto>() }.getOrElse {
            appLogger.warn("Refresh response did not match the contract; session kept")
            return null
        }
        return when (val refreshed = tokens.toSession(email = current.email, logger = appLogger)) {
            is Result.Success -> {
                sessionStorage.set(refreshed.data)
                refreshed.data.toBearerTokens()
            }
            is Result.Failure -> {
                // The server issued a token that says nothing about who this is. Nothing the
                // app could do with it, and the old refresh token is already spent.
                sessionEvents.forceSignOut(SignOutReason.SESSION_REJECTED)
                null
            }
        }
    }

    private fun Session.toBearerTokens() = BearerTokens(accessToken = accessToken, refreshToken = refreshToken)

    private fun Url.isPublicAuthRoute(): Boolean =
        AuthRoutes.PUBLIC_ROUTES.any { route -> encodedPath.endsWith(route) }

    private companion object {
        const val API_KEY_HEADER = "x-api-key"
        const val CODE_ACCESS_EXPIRED = "TOKEN_ACCESO_EXPIRADO"
        const val CODE_NOT_AUTHENTICATED = "NO_AUTENTICADO"
        const val CODE_REFRESH_INVALID = "TOKEN_RENOVACION_INVALIDO"
        const val CODE_EMPLOYEE_INACTIVE = "EMPLEADO_INACTIVO"
    }
}
