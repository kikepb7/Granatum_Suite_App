package com.granatum.core.domain.auth.model

/** A ready-made account of the Demo environment: the login offers to sign in with it in one tap. */
data class DemoAccount(val role: UserRole, val displayName: String, val email: String, val password: String) {
    override fun toString(): String = "DemoAccount(role=$role, email=$email, password=***)"
}
