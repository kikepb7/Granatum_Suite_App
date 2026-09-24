package com.granatum.core.data.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthInfoSerializableDTO(
    val accessToken: String,
    val refreshToken: String,
    val user: UserSerializableDTO
)
