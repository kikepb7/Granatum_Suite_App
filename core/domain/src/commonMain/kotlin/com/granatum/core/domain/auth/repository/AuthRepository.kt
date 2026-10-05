package com.granatum.core.domain.auth.repository

import com.granatum.core.domain.auth.model.AuthInfoModel
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result

/**
 * Granatum accounts are created by the company, so there is deliberately no self-service here:
 * no sign-up, no email verification, no password recovery. Those flows came from the skeleton
 * this app started as and were removed rather than hidden, so nobody has to reason about dead
 * paths when changing authentication.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthInfoModel, DataError.Remote>
    suspend fun changePassword(currentPassword: String, newPassword: String): EmptyResult<DataError.Remote>
    suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote>
}
