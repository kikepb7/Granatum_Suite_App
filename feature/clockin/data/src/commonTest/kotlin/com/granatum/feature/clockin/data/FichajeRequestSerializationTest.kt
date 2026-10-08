package com.granatum.feature.clockin.data

import com.granatum.feature.clockin.data.dto.EntradaRequestDto
import com.granatum.feature.clockin.data.dto.InicioPausaRequestDto
import com.granatum.feature.clockin.data.remote.FichajeRemoteDataSource
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * research D5: a retried punch has to send exactly the same body, or the server answers
 * CLIENT_EVENT_ID_REUTILIZADO and the punch is lost.
 */
class FichajeRequestSerializationTest {

    @Test
    fun the_same_instant_always_formats_the_same() {
        val ms = 1_791_446_131_596L
        assertEquals(FichajeRemoteDataSource.formatOccurredAt(ms), FichajeRemoteDataSource.formatOccurredAt(ms))
        assertEquals("1970-01-01T00:00:00.123Z", FichajeRemoteDataSource.formatOccurredAt(123L))
    }

    @Test
    fun a_body_serialises_byte_for_byte_identically() {
        val at = FichajeRemoteDataSource.formatOccurredAt(1_791_446_131_596L)
        val a = Json.encodeToString(InicioPausaRequestDto("e-1", at, "COMIDA"))
        val b = Json.encodeToString(InicioPausaRequestDto("e-1", at, "COMIDA"))
        assertEquals(a, b)
        assertEquals("""{"clientEventId":"e-1","occurredAt":"$at","tipo":"COMIDA"}""", a)
    }

    @Test
    fun only_the_break_start_carries_a_type() {
        assertEquals("""{"clientEventId":"e-1","occurredAt":"x"}""", Json.encodeToString(EntradaRequestDto("e-1", "x")))
    }
}
