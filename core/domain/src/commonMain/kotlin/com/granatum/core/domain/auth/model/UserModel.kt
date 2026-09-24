package com.granatum.core.domain.auth.model

data class UserModel(
    val id: String,
    val email: String,
    val username: String,
    val hasVerifiedEmail: Boolean,
    val profilePictureUrl: String?,
    val role: UserRole
)
