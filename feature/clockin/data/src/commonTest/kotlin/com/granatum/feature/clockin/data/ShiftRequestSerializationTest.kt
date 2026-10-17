package com.granatum.feature.clockin.data

import com.granatum.feature.clockin.data.dto.BreakStartRequestDto
import com.granatum.feature.clockin.data.dto.ClockInRequestDto
import com.granatum.feature.clockin.data.remote.ShiftRemoteDataSource
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * research D5: a retried punch has to send exactly the same body, or the server answers
 * CLIENT_EVENT_ID_REUSED and the punch is lost.
 */
class ShiftRequestSerializationTest {

    @Test
    fun the_same_instant_always_formats_the_same() {
        val ms = 1_791_446_131_596L
        assertEquals(ShiftRemoteDataSource.formatOccurredAt(ms), ShiftRemoteDataSource.formatOccurredAt(ms))
        assertEquals("1970-01-01T00:00:00.123Z", ShiftRemoteDataSource.formatOccurredAt(123L))
    }

    @Test
    fun a_body_serialises_byte_for_byte_identically() {
        val at = ShiftRemoteDataSource.formatOccurredAt(1_791_446_131_596L)
        val a = Json.encodeToString(BreakStartRequestDto("e-1", at, "MEAL"))
        val b = Json.encodeToString(BreakStartRequestDto("e-1", at, "MEAL"))
        assertEquals(a, b)
        assertEquals("""{"clientEventId":"e-1","occurredAt":"$at","type":"MEAL"}""", a)
    }

    @Test
    fun only_the_break_start_carries_a_type() {
        assertEquals("""{"clientEventId":"e-1","occurredAt":"x"}""", Json.encodeToString(ClockInRequestDto("e-1", "x")))
    }
}
