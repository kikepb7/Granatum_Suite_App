package com.granatum.core.data.auth.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDTO(
    val username: String,
    val email: String,
    val password: String,
)
