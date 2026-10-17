package com.granatum.feature.events.domain

import com.granatum.feature.events.domain.model.EventDraft
import com.granatum.feature.events.domain.validation.EventIssue
import com.granatum.feature.events.domain.validation.EventValidation
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class EventValidationTest {

    private val today = LocalDate(2027, 6, 13)
    private val sut = EventValidation

    private fun draft(name: String = "Evento 1", description: String? = "Boda de Juan y María", date: LocalDate = today) =
        EventDraft(name, description, date)

    @Test
    fun `When the event is complete and for today then it has no issues`() {
        val expected = emptySet<EventIssue>()

        val actual = sut.validate(draft(), today)

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `When the name is blank and the date has gone then both are reported`() {
        val expected = setOf(EventIssue.NAME_REQUIRED, EventIssue.DATE_IN_PAST)

        val actual = sut.validate(draft(name = "   ", date = today.minusDays(1)), today)

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `When the texts exceed the server limits then they are reported`() {
        val expected = setOf(EventIssue.NAME_TOO_LONG, EventIssue.DESCRIPTION_TOO_LONG)

        val actual = sut.validate(draft(name = "a".repeat(101), description = "b".repeat(501)), today)

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `When the texts are exactly at the limits then they are accepted`() {
        val expected = emptySet<EventIssue>()

        val actual = sut.validate(draft(name = "a".repeat(100), description = "b".repeat(500)), today)

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `When there are three events then the suggested name is the fourth`() {
        val expected = "Evento 4"

        val actual = sut.suggestedName(existing = 3)

        assertEquals(expected = expected, actual = actual)
    }

    private fun LocalDate.minusDays(days: Int) = LocalDate.fromEpochDays(toEpochDays() - days)
}
