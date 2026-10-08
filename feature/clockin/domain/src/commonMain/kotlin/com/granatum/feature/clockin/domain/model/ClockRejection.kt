package com.granatum.feature.clockin.domain.model

import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Why the server refused a punch for good (specs/005-fichaje-real, research D4). Stored as the
 * server's `code`; this is what the screen explains.
 */
enum class ClockRejection {
    /** Older than the server's 72-hour window: it can only be fixed by a correction. */
    TooOld,

    /** The device clock is ahead of the server's: every punch will keep failing until it is fixed. */
    ClockSkew,
    Inactive,

    /** The register no longer allowed it, typically because another device punched first. */
    InvalidTransition,

    /** Its shift's entry could not be registered, so nothing that depends on it can be. */
    ShiftNotRegistered,
    Unexpected;

    companion object {
        /** Not a server code: the local cascade when a shift's entry was rejected. */
        const val SHIFT_NOT_REGISTERED = "JORNADA_NO_REGISTRADA"

        private val PAST_TOLERANCE = 72.hours

        fun fromServerCode(code: String, occurredAt: Instant, now: Instant): ClockRejection = when (code) {
            // The server uses one code for both directions; the age of the punch tells them apart.
            "DESVIACION_RELOJ" -> if (now - occurredAt > PAST_TOLERANCE) TooOld else ClockSkew
            "EMPLEADO_INACTIVO" -> Inactive
            "PAUSA_YA_ABIERTA", "PAUSA_NO_ABIERTA", "PAUSA_ABIERTA_AL_CERRAR", "FICHAJE_NO_EN_CURSO" -> InvalidTransition
            SHIFT_NOT_REGISTERED -> ShiftNotRegistered
            else -> Unexpected
        }
    }
}
