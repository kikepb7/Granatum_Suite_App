package com.granatum.core.data.mappers

import com.granatum.core.data.auth.dto.AuthInfoSerializableDTO
import com.granatum.core.data.auth.dto.UserRoleDto
import com.granatum.core.data.auth.dto.UserSerializableDTO
import com.granatum.core.domain.auth.model.AuthInfoModel
import com.granatum.core.domain.auth.model.UserModel
import com.granatum.core.domain.auth.model.UserRole

fun AuthInfoSerializableDTO.toDomain(): AuthInfoModel {
    return AuthInfoModel(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = user.toDomain()
    )
}

fun UserSerializableDTO.toDomain(): UserModel {
    return UserModel(
        id = id,
        email = email,
        username = username,
        hasVerifiedEmail = hasVerifiedEmail,
        profilePictureUrl = profilePictureUrl,
        role = role.toDomain()
    )
}

fun UserModel.toDto(): UserSerializableDTO {
    return UserSerializableDTO(
        id = id,
        email = email,
        username = username,
        hasVerifiedEmail = hasVerifiedEmail,
        profilePictureUrl = profilePictureUrl,
        role = role.toDto()
    )
}

fun UserRoleDto.toDomain(): UserRole = when (this) {
    UserRoleDto.ADMIN -> UserRole.ADMIN
    UserRoleDto.ENCARGADO -> UserRole.ENCARGADO
    UserRoleDto.EMPLEADO -> UserRole.EMPLEADO
}

fun UserRole.toDto(): UserRoleDto = when (this) {
    UserRole.ADMIN -> UserRoleDto.ADMIN
    UserRole.ENCARGADO -> UserRoleDto.ENCARGADO
    UserRole.EMPLEADO -> UserRoleDto.EMPLEADO
}

fun AuthInfoModel.toDto(): AuthInfoSerializableDTO {
    return AuthInfoSerializableDTO(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = user.toDto()
    )
}