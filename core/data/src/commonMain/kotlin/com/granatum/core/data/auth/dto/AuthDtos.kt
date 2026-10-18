package com.granatum.core.data.auth.dto

import kotlinx.serialization.Serializable

// Field names match docs/openapi.json letter for letter (constitution principle VI).

@Serializable
data class TokenPairResponseDto(
    val accessToken: String,
    val refreshToken: String,
    /** Seconds. Not stored: expiry is the server's call, not the device clock's. */
    val expiresIn: Long,
    val passwordChangeRequired: Boolean
) {
    override fun toString(): String =
        "TokenPairResponseDto(accessToken=***, refreshToken=***, expiresIn=$expiresIn, " +
            "passwordChangeRequired=$passwordChangeRequired)"
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

/** The owner's sign-up (`SignUpRequest`): every field required, the code included. */
@Serializable
data class SignUpRequestDto(
    val email: String,
    val password: String,
    val name: String,
    val lastName: String,
    val identityDocument: String,
    val bootstrapCode: String
) {
    override fun toString(): String = "SignUpRequestDto(email=$email, password=***, bootstrapCode=***)"
}

/** `SignUpResponse`. Not read today: a 2xx is all the app needs before signing in. */
@Serializable
data class SignUpResponseDto(val status: String, val message: String)

@Serializable
data class ChangePasswordRequestDto(val currentPassword: String, val newPassword: String) {
    override fun toString(): String = "ChangePasswordRequestDto(currentPassword=***, newPassword=***)"
}

/**
 * The backend's single error shape. `requirements` only comes with `422 WEAK_PASSWORD`, the one
 * declared exception to `{code, message}`.
 */
@Serializable
data class ErrorBodyDto(
    val code: String,
    val message: String = "",
    val requirements: List<String>? = null
)
