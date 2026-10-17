package com.granatum.core.domain.validation

/**
 * The backend's password policy (its spec 002, FR-023), checked before sending so the person
 * sees what is missing while typing. The server still has the last word: if it rejects a
 * password this accepted, its `422` lists the requirements and the screen shows those.
 */
object PasswordValidator {
    const val MIN_LENGTH = 8

    /** The request field's limit in the API contract. */
    const val MAX_LENGTH = 128

    /** The unmet requirements; empty means the password is acceptable. */
    fun validate(password: String): Set<PasswordRequirement> = buildSet {
        if (password.length < MIN_LENGTH) add(PasswordRequirement.MIN_LENGTH)
        if (password.length > MAX_LENGTH) add(PasswordRequirement.MAX_LENGTH)
        if (password.none { it.isUpperCase() }) add(PasswordRequirement.MISSING_UPPERCASE)
        if (password.none { it.isLowerCase() }) add(PasswordRequirement.MISSING_LOWERCASE)
        if (password.none { it.isDigit() }) add(PasswordRequirement.MISSING_DIGIT)
        if (password.none { !it.isLetterOrDigit() && !it.isWhitespace() }) add(PasswordRequirement.MISSING_SYMBOL)
    }
}
