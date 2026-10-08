package com.granatum.feature.clockin.domain

import com.granatum.feature.clockin.domain.model.ClockRejection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ClockRejectionTest {
    private val now = Instant.parse("2026-10-08T10:00:00Z")

    @Test
    fun clock_deviation_means_too_old_beyond_72_hours_and_skew_otherwise() {
        assertEquals(ClockRejection.TooOld, ClockRejection.fromServerCode("DESVIACION_RELOJ", now - 73.hours, now))
        assertEquals(ClockRejection.ClockSkew, ClockRejection.fromServerCode("DESVIACION_RELOJ", now - 71.hours, now))
        assertEquals(ClockRejection.ClockSkew, ClockRejection.fromServerCode("DESVIACION_RELOJ", now + 10.minutes, now))
    }

    @Test
    fun every_documented_code_maps() {
        assertEquals(ClockRejection.Inactive, ClockRejection.fromServerCode("EMPLEADO_INACTIVO", now, now))
        listOf("PAUSA_YA_ABIERTA", "PAUSA_NO_ABIERTA", "PAUSA_ABIERTA_AL_CERRAR", "FICHAJE_NO_EN_CURSO").forEach {
            assertEquals(ClockRejection.InvalidTransition, ClockRejection.fromServerCode(it, now, now), it)
        }
        assertEquals(ClockRejection.ShiftNotRegistered, ClockRejection.fromServerCode(ClockRejection.SHIFT_NOT_REGISTERED, now, now))
        assertEquals(ClockRejection.Unexpected, ClockRejection.fromServerCode("CLIENT_EVENT_ID_REUTILIZADO", now, now))
        assertEquals(ClockRejection.Unexpected, ClockRejection.fromServerCode("ALGO_NUEVO", now, now))
    }
}
