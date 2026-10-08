package com.granatum.core.domain.auth.model

/** What the owner types to sign up (backend feature 009). Never logged: it holds a password. */
class OwnerRegistration(
    val name: String,
    val identityDocument: String,
    val email: String,
    val password: String,
    val bootstrapCode: String
) {
    override fun toString(): String = "OwnerRegistration(email=$email, password=***, bootstrapCode=***)"

    companion object {
        const val MAX_NAME = 150
        const val MAX_DOCUMENT = 20
        const val MAX_EMAIL = 254
        const val MAX_CODE = 200
    }
}
