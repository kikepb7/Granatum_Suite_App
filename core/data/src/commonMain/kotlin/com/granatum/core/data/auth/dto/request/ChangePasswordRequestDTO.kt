package com.granatum.core.data.auth.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequestDTO(
    val currentPassword: String,
    val newPassword: String
)
