package com.granatum.core.data.auth.dto

import kotlinx.serialization.Serializable

// Field names match docs/openapi.json letter for letter (constitution principle VI). They are
// Spanish where the backend's are; renaming them here would only move the mismatch.

@Serializable
data class ParTokensResponseDto(
    val accessToken: String,
    val refreshToken: String,
    /** Seconds. Not stored: expiry is the server's call, not the device clock's. */
    val expiresIn: Long,
    val requiereCambioPassword: Boolean
) {
    override fun toString(): String =
        "ParTokensResponseDto(accessToken=***, refreshToken=***, expiresIn=$expiresIn, " +
            "requiereCambioPassword=$requiereCambioPassword)"
}

@Serializable
data class LoginRequestDto(val email: String, val password: String) {
    override fun toString(): String = "LoginRequestDto(email=$email, password=***)"
}

@Serializable
data class RefreshRequestDto(val refreshToken: String) {
    override fun toString(): String = "RefreshRequestDto(refreshToken=***)"
}

@Serializable
data class LogoutRequestDto(val refreshToken: String) {
    override fun toString(): String = "LogoutRequestDto(refreshToken=***)"
}

@Serializable
data class CambioPasswordRequestDto(val passwordActual: String, val passwordNueva: String) {
    override fun toString(): String = "CambioPasswordRequestDto(passwordActual=***, passwordNueva=***)"
}

/**
 * The backend's single error shape. `requisitos` only comes with `422 PASSWORD_DEBIL`, the one
 * declared exception to `{code, message}`.
 */
@Serializable
data class ErrorBodyDto(
    val code: String,
    val message: String = "",
    val requisitos: List<String>? = null
)
