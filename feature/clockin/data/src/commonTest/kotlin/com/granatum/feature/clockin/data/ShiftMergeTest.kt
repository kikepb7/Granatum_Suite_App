package com.granatum.feature.clockin.data

import com.granatum.feature.clockin.data.merge.ShiftMerger
import com.granatum.feature.clockin.database.entity.ClockEventEntity
import com.granatum.feature.clockin.database.entity.ServerShiftEntity
import com.granatum.feature.clockin.database.entity.ShiftEntity
import com.granatum.feature.clockin.domain.model.ClockEventType
import com.granatum.feature.clockin.domain.model.ClockRejection
import com.granatum.feature.clockin.domain.model.ShiftState
import com.granatum.feature.clockin.domain.model.SyncState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/** specs/005-fichaje-real, research D6-D7. */
class ShiftMergeTest {
    private fun t(hhmm: String) = Instant.parse("2026-10-08T$hhmm:00Z")
    private fun ms(hhmm: String) = t(hhmm).toEpochMilliseconds()
    private val now = t("18:00")

    private fun server(id: String, estado: String, entrada: String, salida: String? = null, minutos: Int? = null, pausas: String = "[]") =
        ServerShiftEntity(id, "ana", ms(entrada), salida?.let(::ms), estado, minutos, false, pausas, fetchedAtEpochMillis = 0)

    private fun event(id: String, type: String, at: String, shift: String, state: String = "PENDING", code: String? = null) =
        ClockEventEntity(id, type, ms(at), null, state, 0, null, "ana", shift, null, code)

    @Test
    fun a_server_shift_alone_is_shown_as_the_server_has_it() {
        val pausas = """[{"id":"p","inicio":"2026-10-08T12:00:00Z","fin":"2026-10-08T12:30:00Z","tipo":"COMIDA"}]"""
        val shift = ShiftMerger.merge(listOf(server("srv", "CERRADO", "08:00", "16:00", 450, pausas)), emptyList(), emptyList(), now).single()
        assertEquals(listOf(ClockEventType.CLOCK_IN, ClockEventType.BREAK_START, ClockEventType.BREAK_END, ClockEventType.CLOCK_OUT), shift.items.map { it.type })
        assertEquals(ShiftState.CLOSED, shift.state)
        assertEquals(450, shift.workedMinutes)
        assertFalse(shift.isWorkedMinutesProvisional)
        assertTrue(shift.canRequestCorrection)
    }

    @Test
    fun a_local_shift_unknown_to_the_server_is_provisional() {
        val shift = ShiftMerger.merge(
            emptyList(),
            listOf(ShiftEntity("loc", "ana", null, ms("08:00"))),
            listOf(event("1", "CLOCK_IN", "08:00", "loc"), event("2", "CLOCK_OUT", "10:00", "loc")),
            now
        ).single()
        assertEquals("loc", shift.key)
        assertEquals(120, shift.workedMinutes)
        assertTrue(shift.isWorkedMinutesProvisional)
        assertFalse(shift.canRequestCorrection)
        assertTrue(shift.hasPending)
    }

    @Test
    fun pending_punches_go_on_top_of_the_server_copy() {
        val shift = ShiftMerger.merge(
            listOf(server("srv", "EN_CURSO", "08:00")),
            listOf(ShiftEntity("loc", "ana", "srv", ms("08:00"))),
            listOf(event("1", "CLOCK_IN", "08:00", "loc", state = "SYNCED"), event("2", "CLOCK_OUT", "16:00", "loc")),
            now
        ).single()
        assertEquals("srv", shift.key)
        assertEquals(listOf(SyncState.SYNCED, SyncState.PENDING), shift.items.map { it.syncState })
        assertEquals(ShiftState.CLOSED, shift.state)
        assertEquals(480, shift.workedMinutes)
        assertTrue(shift.isWorkedMinutesProvisional)
    }

    @Test
    fun an_open_shift_from_another_device_is_in_progress() {
        val shift = ShiftMerger.merge(listOf(server("srv", "EN_CURSO", "08:00")), emptyList(), emptyList(), now).single()
        assertEquals(ShiftState.IN_PROGRESS, shift.state)
        assertEquals(600, shift.workedMinutes)
    }

    @Test
    fun rejected_punches_are_shown_but_do_not_count() {
        val shift = ShiftMerger.merge(
            emptyList(),
            listOf(ShiftEntity("loc", "ana", null, ms("08:00"))),
            listOf(event("1", "CLOCK_IN", "08:00", "loc", "REJECTED", "DESVIACION_RELOJ")),
            now
        ).single()
        assertEquals(ClockRejection.ClockSkew, shift.rejections.single().rejection)
        assertEquals(ShiftState.CLOSED, shift.state)
        assertEquals(0, shift.workedMinutes)
    }

    @Test
    fun shifts_come_out_in_order() {
        val merged = ShiftMerger.merge(
            listOf(server("late", "CERRADO", "14:00", "15:00"), server("early", "CERRADO", "08:00", "09:00")),
            emptyList(), emptyList(), now
        )
        assertEquals(listOf("early", "late"), merged.map { it.key })
    }
}
