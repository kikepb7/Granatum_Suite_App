package com.granatum.core.data.mappers

import com.granatum.core.data.auth.dto.ParTokensResponseDto
import com.granatum.core.data.auth.dto.StoredSessionDto
import com.granatum.core.data.auth.token.AccessTokenClaims
import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.logger.AppLogger
import com.granatum.core.domain.util.Result

/**
 * Builds a session from a token pair. Identity and role come from the access token's claims
 * (research D1); a token without a subject is a failed sign-in, never a default role.
 *
 * The pending-change flag is the stricter of the response field and the claim: if they ever
 * disagreed, assuming "no change needed" would show screens the server will refuse.
 */
fun ParTokensResponseDto.toSession(email: String, logger: AppLogger): Result<Session, AuthError> {
    val claims = AccessTokenClaims.parse(accessToken) ?: return Result.Failure(AuthError.InvalidSession)
    val role = UserRole.fromBackend(claims.role)
    if (role == UserRole.DESCONOCIDO) {
        // The claim value is not a secret; the token is, and never goes to the log.
        logger.warn("Unknown role '${claims.role}' in the access token; granting least privilege")
    }
    return Result.Success(
        Session(
            accessToken = accessToken,
            refreshToken = refreshToken,
            employeeId = claims.employeeId,
            role = role,
            mustChangePassword = requiereCambioPassword || claims.pwdChange,
            email = email
        )
    )
}

fun Session.toStored(): StoredSessionDto = StoredSessionDto(
    accessToken = accessToken,
    refreshToken = refreshToken,
    employeeId = employeeId,
    role = role.name,
    mustChangePassword = mustChangePassword,
    email = email
)

fun StoredSessionDto.toDomain(): Session = Session(
    accessToken = accessToken,
    refreshToken = refreshToken,
    employeeId = employeeId,
    role = UserRole.fromBackend(role),
    mustChangePassword = mustChangePassword,
    email = email
)

/** The email as sent and shown: trimmed and lower-cased. Passwords are never transformed. */
fun normaliseEmail(email: String): String = email.trim().lowercase()
