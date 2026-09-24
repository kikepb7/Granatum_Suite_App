package com.granatum.core.data.networking

import com.granatum.core.data.BuildKonfig
import com.granatum.core.data.auth.dto.AuthInfoSerializableDTO
import com.granatum.core.data.auth.dto.request.RefreshRequestDTO
import com.granatum.core.data.mappers.toDomain
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.logger.AppLogger
import com.granatum.core.domain.util.onFailure
import com.granatum.core.domain.util.onSuccess
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

class HttpClientFactory(
    private val appLogger: AppLogger,
    private val sessionStorage: SessionStorage
) {

    fun create(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine = engine) {
            install(ContentNegotiation) {
                json(
                    json = Json {
                        ignoreUnknownKeys = true
                    }
                )
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
            }
            install(WebSockets) {
                pingIntervalMillis = 20_000L
            }
            defaultRequest {
                header("x-api-key", BuildKonfig.API_KEY)
                contentType(ContentType.Application.Json)
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        sessionStorage
                            .observeAuthInfo()
                            .firstOrNull()
                            ?.let {
                                BearerTokens(
                                    accessToken = it.accessToken,
                                    refreshToken = it.refreshToken
                                )
                            }
                    }
                    refreshTokens {
                        if (response.request.url.encodedPath.contains("auth/")) return@refreshTokens null

                        val authInfo = sessionStorage
                            .observeAuthInfo()
                            .firstOrNull()

                        if (authInfo?.refreshToken.isNullOrBlank()) {
                            sessionStorage.set(null)
                            return@refreshTokens null
                        }

                        var bearerTokens: BearerTokens? = null
                        client.post<RefreshRequestDTO, AuthInfoSerializableDTO>(
                            route = "/auth/refresh",
                            body = RefreshRequestDTO(
                                refreshToken = authInfo.refreshToken
                            ),
                            builder = {
                                markAsRefreshTokenRequest()
                            }
                        ).onSuccess { newAuthInfo ->
                            sessionStorage.set(newAuthInfo.toDomain())
                            bearerTokens = BearerTokens(
                                accessToken = newAuthInfo.accessToken,
                                refreshToken = newAuthInfo.refreshToken
                            )
                        }.onFailure { error ->
                            sessionStorage.set(null)
                        }

                        bearerTokens
                    }
                }
            }
        }
    }
}