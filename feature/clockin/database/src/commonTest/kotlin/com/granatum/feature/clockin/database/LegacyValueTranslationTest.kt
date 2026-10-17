package com.granatum.feature.clockin.database

import com.granatum.feature.clockin.database.migration.LegacyValueTranslation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Migration 3 -> 4: the stored Spanish values and their English counterparts. */
class LegacyValueTranslationTest {

    @Test
    fun break_types_and_shift_statuses_follow_the_contract() {
        assertEquals(mapOf("COMIDA" to "MEAL", "DESCANSO" to "REST", "OTRO" to "OTHER"), LegacyValueTranslation.breakTypes)
        assertEquals(
            mapOf("EN_CURSO" to "IN_PROGRESS", "CERRADO" to "CLOSED", "INCOMPLETO" to "INCOMPLETE"),
            LegacyValueTranslation.shiftStatuses
        )
    }

    @Test
    fun the_rejection_codes_the_screen_explains_are_translated() {
        val codes = LegacyValueTranslation.rejectionCodes
        assertEquals("SHIFT_NOT_REGISTERED", codes["JORNADA_NO_REGISTRADA"])
        assertEquals("UNREADABLE_RESPONSE", codes["RESPUESTA_ILEGIBLE"])
        assertEquals("CLOCK_SKEW", codes["DESVIACION_RELOJ"])
        assertEquals("EMPLOYEE_INACTIVE", codes["EMPLEADO_INACTIVO"])
        assertEquals("BREAK_ALREADY_OPEN", codes["PAUSA_YA_ABIERTA"])
        assertEquals("BREAK_NOT_OPEN", codes["PAUSA_NO_ABIERTA"])
        assertEquals("BREAK_OPEN_ON_CLOSE", codes["PAUSA_ABIERTA_AL_CERRAR"])
        assertEquals("SHIFT_NOT_IN_PROGRESS", codes["FICHAJE_NO_EN_CURSO"])
        assertEquals("CLIENT_EVENT_ID_REUSED", codes["CLIENT_EVENT_ID_REUTILIZADO"])
    }

    @Test
    fun cached_breaks_get_the_contract_keys_and_types() {
        val legacy = """[{"id":"p1","inicio":"2026-10-08T12:00:00Z","tipo":"COMIDA","fin":"2026-10-08T12:30:00Z"},""" +
            """{"id":"p2","inicio":"2026-10-08T15:00:00Z","tipo":"DESCANSO"},{"id":"p3","inicio":"2026-10-08T16:00:00Z","tipo":"OTRO"}]"""
        val expected = """[{"id":"p1","start":"2026-10-08T12:00:00Z","type":"MEAL","end":"2026-10-08T12:30:00Z"},""" +
            """{"id":"p2","start":"2026-10-08T15:00:00Z","type":"REST"},{"id":"p3","start":"2026-10-08T16:00:00Z","type":"OTHER"}]"""
        assertEquals(expected, LegacyValueTranslation.translateBreaksJson(legacy))
        assertEquals(expected, LegacyValueTranslation.translateBreaksJson(expected), "running it twice changes nothing")
        assertEquals("[]", LegacyValueTranslation.translateBreaksJson("[]"))
    }

    @Test
    fun the_update_only_touches_known_legacy_values() {
        val sql = LegacyValueTranslation.updateValuesSql("clock_event", "breakType", LegacyValueTranslation.breakTypes)
        assertEquals(
            "UPDATE `clock_event` SET `breakType` = CASE `breakType` WHEN 'COMIDA' THEN 'MEAL' WHEN 'DESCANSO' THEN 'REST' " +
                "WHEN 'OTRO' THEN 'OTHER' ELSE `breakType` END WHERE `breakType` IN ('COMIDA', 'DESCANSO', 'OTRO')",
            sql
        )
    }

    @Test
    fun every_server_shift_column_gets_an_english_name() {
        val renamed = LegacyValueTranslation.serverShiftColumns.map { it.second }
        assertEquals(
            listOf("clockInEpochMillis", "clockOutEpochMillis", "status", "workedMinutes", "wasIncomplete", "breaksJson", "corrected", "reconstructed"),
            renamed
        )
        assertTrue(LegacyValueTranslation.serverShiftColumns.none { it.first == it.second })
    }
}
