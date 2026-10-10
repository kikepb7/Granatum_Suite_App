package com.granatum.core.domain.auth.model

/**
 * A ready-made account of the demo build: the login screen offers to sign in with it directly.
 * Only the demo build has any; every other build gets an empty list, so nothing shows.
 */
data class DemoAccount(val role: UserRole, val displayName: String, val email: String, val password: String) {
    override fun toString(): String = "DemoAccount(role=$role, email=$email, password=***)"
}

fun interface DemoAccounts {
    fun accounts(): List<DemoAccount>
}
