package com.granatum.core.domain.auth.repository

import com.granatum.core.domain.auth.AuthError
import com.granatum.core.domain.auth.model.Session
import com.granatum.core.domain.util.Result

interface AuthRepository {
    /** Stores the session on success. */
    suspend fun login(email: String, password: String): Result<Session, AuthError>

    /** Stores the new session the server returns, which no longer requires a change. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Session, AuthError>

    /**
     * Always ends the session on this device. Revoking it on the server is attempted and its
     * outcome ignored: signing out has to work without coverage too.
     */
    suspend fun logout()
}
