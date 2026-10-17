package com.granatum.core.domain.validation

/**
 * Named exactly like the identifiers in the backend's `422 WEAK_PASSWORD` body, so the list
 * checked locally and the list returned by the server are rendered by the same code.
 */
enum class PasswordRequirement {
    MIN_LENGTH,
    MAX_LENGTH,
    MISSING_UPPERCASE,
    MISSING_LOWERCASE,
    MISSING_DIGIT,
    MISSING_SYMBOL;

    companion object {
        fun fromBackend(name: String): PasswordRequirement? = entries.firstOrNull { it.name == name }
    }
}
