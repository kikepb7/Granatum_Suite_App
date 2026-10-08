package com.granatum.core.data.auth.dto

import kotlinx.serialization.Serializable

/**
 * The session as written to the secure store, under `granatum.session.secure.v2`. Its own type
 * rather than the domain model, so a rename in domain can never silently change what is on
 * disk.
 */
@Serializable
data class StoredSessionDto(
    val accessToken: String,
    val refreshToken: String,
    val employeeId: String,
    val role: String,
    val mustChangePassword: Boolean,
    val email: String
) {
    override fun toString(): String =
        "StoredSessionDto(accessToken=***, refreshToken=***, employeeId=$employeeId, role=$role, " +
            "mustChangePassword=$mustChangePassword)"
}
