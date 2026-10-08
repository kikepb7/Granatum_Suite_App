package com.granatum.core.domain.auth.model

/**
 * What the app keeps between launches, in the secure store only (constitution principle VII).
 *
 * [employeeId], [role] and [mustChangePassword] are read from the access token's claims: the
 * login response carries tokens and nothing else, and the backend offers no "who am I" route.
 * [email] is what the person typed, kept only to show back to them.
 *
 * The access token's expiry is deliberately absent. Whether a session has expired is the
 * server's call (`TOKEN_ACCESO_EXPIRADO`), never the device clock's.
 */
data class Session(
    val accessToken: String,
    val refreshToken: String,
    val employeeId: String,
    val role: UserRole,
    val mustChangePassword: Boolean,
    val email: String
)
