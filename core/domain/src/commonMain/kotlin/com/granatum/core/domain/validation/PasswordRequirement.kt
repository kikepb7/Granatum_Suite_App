package com.granatum.core.domain.validation

/**
 * Named exactly like the identifiers in the backend's `422 PASSWORD_DEBIL` body, so the list
 * checked locally and the list returned by the server are rendered by the same code.
 */
enum class PasswordRequirement {
    LONGITUD_MINIMA,
    LONGITUD_MAXIMA,
    FALTA_MAYUSCULA,
    FALTA_MINUSCULA,
    FALTA_DIGITO,
    FALTA_SIMBOLO;

    companion object {
        fun fromBackend(name: String): PasswordRequirement? = entries.firstOrNull { it.name == name }
    }
}
