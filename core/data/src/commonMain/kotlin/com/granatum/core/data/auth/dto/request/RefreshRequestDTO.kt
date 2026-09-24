package com.granatum.core.data.auth.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class RefreshRequestDTO(
    val refreshToken: String
)