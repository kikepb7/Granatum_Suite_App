package com.granatum.core.data.auth

import com.granatum.core.data.auth.dto.AuthInfoSerializableDTO
import com.granatum.core.data.auth.dto.request.ChangePasswordRequestDTO
import com.granatum.core.data.auth.dto.request.LoginRequestDTO
import com.granatum.core.data.auth.dto.request.RefreshRequestDTO
import com.granatum.core.data.auth.provider.AuthRoutes.LOGIN_ROUTE
import com.granatum.core.data.mappers.toDomain
import com.granatum.core.data.networking.post
import com.granatum.core.domain.auth.model.AuthInfoModel
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.util.DataError
import com.granatum.core.domain.util.EmptyResult
import com.granatum.core.domain.util.Result
import com.granatum.core.domain.util.map
import com.granatum.core.domain.util.onSuccess
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider

class KtorAuthRepositoryImpl(
    private val httpClient: HttpClient
): AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): Result<AuthInfoModel, DataError.Remote> =
        httpClient.post<LoginRequestDTO, AuthInfoSerializableDTO>(
            route = LOGIN_ROUTE,
            body = LoginRequestDTO(
                email = email,
                password = password
            )
        ).map { authInfoSerializable ->
            authInfoSerializable.toDomain()
        }

    override suspend fun changePassword(currentPassword: String, newPassword: String): EmptyResult<DataError.Remote> =
        httpClient.post(
            route = "/auth/change-password",
            body = ChangePasswordRequestDTO(
                currentPassword = currentPassword,
                newPassword = newPassword
            )
        )

    override suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote> =
        httpClient.post<RefreshRequestDTO, Unit>(
            route = "/auth/logout",
            body = RefreshRequestDTO(refreshToken = refreshToken)
        ).onSuccess {
            httpClient.authProvider<BearerAuthProvider>()?.clearToken()
        }
}
