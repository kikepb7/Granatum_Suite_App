package com.granatum.core.data.auth.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDTO(
    val email: String,
    val password: String
)
