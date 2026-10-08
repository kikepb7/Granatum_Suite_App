package com.granatum.core.data.auth.token

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The claims of the backend's access token that the app relies on, as documented in the
 * backend's `docs/ARCHITECTURE.md` ("Seguridad y roles") — see docs/api-contract.md.
 *
 * The signature is NOT verified. That is the server's job and the app does not hold the key; a
 * tampered token would only unlock screens whose every request the server still rejects.
 */
data class AccessTokenClaims(
    /** `sub`: the id of the person employed, not of the account. */
    val employeeId: String,
    /** `role`, as the backend spells it. Mapped to a domain role by the caller. */
    val role: String?,
    /** `pwd_change`: the session may only change its password. Absent means false. */
    val pwdChange: Boolean
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        @OptIn(ExperimentalEncodingApi::class)
        private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

        /** Null when the token is not a JWT, its payload does not parse, or it has no subject. */
        @OptIn(ExperimentalEncodingApi::class)
        fun parse(token: String): AccessTokenClaims? {
            val segments = token.split('.')
            if (segments.size != 3) return null
            return runCatching {
                val payload = base64Url.decode(segments[1]).decodeToString()
                json.decodeFromString<Payload>(payload)
            }.getOrNull()
                ?.takeIf { !it.sub.isNullOrBlank() }
                ?.let { AccessTokenClaims(employeeId = it.sub!!, role = it.role, pwdChange = it.pwdChange == true) }
        }
    }

    @Serializable
    private data class Payload(
        val sub: String? = null,
        val role: String? = null,
        @SerialName("pwd_change") val pwdChange: Boolean? = null
    )
}
