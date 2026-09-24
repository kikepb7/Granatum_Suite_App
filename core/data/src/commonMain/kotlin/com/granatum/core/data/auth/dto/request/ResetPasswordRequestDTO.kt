package com.granatum.core.data.auth.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class ResetPasswordRequestDTO(
    val newPassword: String,
    val token: String
)
