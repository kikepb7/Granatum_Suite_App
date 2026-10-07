package com.granatum.core.data.auth

import com.granatum.core.data.BuildKonfig
import com.granatum.core.data.networking.UrlConstants
import com.granatum.core.domain.auth.model.AuthInfoModel
import com.granatum.core.domain.auth.model.UserModel
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.logger.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.parameter

private const val LOCAL_ENVIRONMENT = "local"
private const val DEV_TOKEN_PATH = "/dev/token"

/**
 * Loads a session for a given role so role-gated navigation can be exercised while there is
 * still no login screen — the backend has no authentication module yet, only a development
 * token endpoint.
 *
 * **Verification scaffold, not a way in.** It is gated here rather than at the call site, so
 * no staging or production build can be made to run it however it gets wired up later, and it
 * appears nowhere in the UI. Pick a role at build time:
 *
 * ```
 * ./gradlew :composeApp:assembleDebug -Pbuildkonfig.flavor=local -PDEV_SESSION_ROLE=EMPLEADO
 * ```
 *
 * Does nothing unless both the environment is local and a role was asked for.
 */
class DevSessionSeeder(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
    private val logger: AppLogger
) {

    suspend fun seed() {
        if (BuildKonfig.ENVIRONMENT != LOCAL_ENVIRONMENT) return

        val role = BuildKonfig.DEV_SESSION_ROLE.takeIf { it.isNotBlank() } ?: return
        val userRole = UserRole.entries.firstOrNull { it.name == role } ?: run {
            logger.warn("Unknown DEV_SESSION_ROLE '$role'; expected one of ${UserRole.entries.joinToString { it.name }}")
            return
        }

        runCatching {
            val response: DevTokenResponse = httpClient.post(devTokenUrl()) {
                parameter("role", userRole.name)
            }.body()

            sessionStorage.set(
                AuthInfoModel(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    user = UserModel(
                        id = response.subject,
                        email = "dev@granatum.local",
                        username = "dev-${userRole.name.lowercase()}",
                        hasVerifiedEmail = true,
                        profilePictureUrl = null,
                        role = userRole
                    )
                )
            )
            logger.info("Seeded a development session for role ${userRole.name}")
        }.onFailure { throwable ->
            logger.warn("Could not seed a development session: ${throwable.message}")
        }
    }

    // The development endpoint hangs off /api, unlike the health check.
    private fun devTokenUrl(): String = UrlConstants.BASE_URL_HTTP.trimEnd('/') + DEV_TOKEN_PATH
}

@kotlinx.serialization.Serializable
data class DevTokenResponse(
    val subject: String,
    val role: String,
    val accessToken: String,
    val refreshToken: String
)
