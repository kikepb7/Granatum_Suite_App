package com.granatum.core.domain.auth.repository

import com.granatum.core.domain.auth.model.AuthInfoModel
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthInfoModel, DataError.Remote>
    suspend fun register(username: String, email: String, password: String): EmptyResult<DataError.Remote>
    suspend fun resendVerificationEmail(email: String): EmptyResult<DataError.Remote>
    suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote>
    suspend fun forgotPassword(email: String): EmptyResult<DataError.Remote>
    suspend fun resetPassword(newPassword: String, token: String): EmptyResult<DataError.Remote>
    suspend fun changePassword(currentPassword: String, newPassword: String): EmptyResult<DataError.Remote>
    suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote>
}