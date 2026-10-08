package com.granatum.feature.clockin.database

import com.granatum.feature.clockin.database.migration.LegacyEvent
import com.granatum.feature.clockin.database.migration.groupLegacyEvents
import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyShiftGroupingTest {
    private var n = 0
    private val ids = { "s${++n}" }
    private fun e(id: String, type: String, at: Long, who: String = "ana") = LegacyEvent(id, who, type, at)

    @Test
    fun every_entry_opens_a_shift_and_what_follows_belongs_to_it() {
        val g = groupLegacyEvents(
            listOf(e("1", "CLOCK_IN", 10), e("2", "BREAK_START", 20), e("3", "BREAK_END", 30), e("4", "CLOCK_OUT", 40), e("5", "CLOCK_IN", 50)),
            ids
        )
        assertEquals(listOf(10L, 50L), g.shifts.map { it.startedAtEpochMillis })
        assertEquals(mapOf("1" to "s1", "2" to "s1", "3" to "s1", "4" to "s1", "5" to "s2"), g.assignments)
        assertEquals(emptyList(), g.orphans)
    }

    @Test
    fun punches_before_the_first_entry_are_orphans() {
        val g = groupLegacyEvents(listOf(e("1", "CLOCK_OUT", 5), e("2", "CLOCK_IN", 10)), ids)
        assertEquals(listOf("1"), g.orphans)
        assertEquals(mapOf("2" to "s1"), g.assignments)
    }

    @Test
    fun order_is_by_time_not_by_input() {
        val g = groupLegacyEvents(listOf(e("b", "BREAK_START", 20), e("a", "CLOCK_IN", 10)), ids)
        assertEquals(mapOf("a" to "s1", "b" to "s1"), g.assignments)
    }

    @Test
    fun people_are_never_mixed() {
        val g = groupLegacyEvents(listOf(e("1", "CLOCK_IN", 10, "ana"), e("2", "CLOCK_OUT", 20, "bea")), ids)
        assertEquals(listOf("2"), g.orphans)
        assertEquals("ana", g.shifts.single().employeeId)
    }
}
