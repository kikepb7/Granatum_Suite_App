package com.granatum.core.domain.auth

import com.granatum.core.domain.util.Error
import com.granatum.core.domain.validation.PasswordRequirement

/**
 * Failures of the auth routes, told apart the way the backend tells them apart: by the stable
 * `code` in its `{code, message}` body, not by HTTP status. `CREDENCIALES_INVALIDAS` and
 * `TOKEN_ACCESO_EXPIRADO` are both a 401 and mean opposite things.
 *
 * A sibling of `DataError` rather than new entries in it: `DataError` stays identical to the
 * reference project's (constitution, principle IV), and it cannot carry `Retry-After` or the
 * list of unmet password requirements.
 */
sealed interface AuthError : Error {
    /** Unknown email, wrong password, inactive person or locked account: one answer for all four. */
    data object InvalidCredentials : AuthError
    data class TooManyAttempts(val retryAfterSeconds: Long?) : AuthError
    data class ServiceBusy(val retryAfterSeconds: Long?) : AuthError
    data class WeakPassword(val requirements: Set<PasswordRequirement>) : AuthError
    data object Validation : AuthError

    /**
     * The owner's sign-up (backend feature 009): a wrong code, no code configured and an ADMIN
     * already existing all get this one answer, on purpose.
     */
    data object InvalidBootstrapCode : AuthError
    data object EmailTaken : AuthError
    /** The staff record with that identity document already has an account. */
    data object AccountExists : AuthError
    /** DNI or NIE with the wrong check letter. */
    data object InvalidDocument : AuthError
    data object NoInternet : AuthError
    data object Timeout : AuthError

    /** The server answered, but its token says nothing usable about who this is. */
    data object InvalidSession : AuthError
    data object Unknown : AuthError
}
