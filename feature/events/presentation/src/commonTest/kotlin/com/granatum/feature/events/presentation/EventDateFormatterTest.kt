package com.granatum.feature.events.presentation

import com.granatum.core.presentation.util.UiText
import com.granatum.feature.events.presentation.common.EventDateFormatter
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.when_days_ago
import granatumsuite.feature.events.presentation.generated.resources.when_in_days
import granatumsuite.feature.events.presentation.generated.resources.when_today
import granatumsuite.feature.events.presentation.generated.resources.when_tomorrow
import granatumsuite.feature.events.presentation.generated.resources.when_yesterday
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class EventDateFormatterTest {

    private val sut = EventDateFormatter()

    @Test
    fun `When dates are formatted then they read the Spanish way`() {
        // Data-driven: kotlin.test has no parameterised tests, and these run on iOS too.
        listOf(
            LocalDate(2027, 6, 13) to "dom, 13 jun 2027",
            LocalDate(2027, 9, 1) to "mié, 1 sept 2027",
            LocalDate(2028, 1, 3) to "lun, 3 ene 2028"
        ).forEach { (date, expected) ->
            assertEquals(expected = expected, actual = sut.long(date), message = date.toString())
        }
    }

    @Test
    fun `When the badge is built then it has the day and the month in capitals`() {
        val date = LocalDate(2027, 6, 3)
        val expected = Triple("3", "JUN", "03/06/2027")

        val actual = Triple(sut.day(date), sut.monthBadge(date), sut.short(date))

        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `When the distance to today is described then each case has its words`() {
        listOf(
            TODAY to UiText.Resource(Res.string.when_today),
            LocalDate(2027, 6, 14) to UiText.Resource(Res.string.when_tomorrow),
            LocalDate(2027, 6, 18) to UiText.Resource(Res.string.when_in_days, arrayOf(5)),
            LocalDate(2027, 6, 12) to UiText.Resource(Res.string.when_yesterday),
            LocalDate(2027, 6, 1) to UiText.Resource(Res.string.when_days_ago, arrayOf(12))
        ).forEach { (date, expected) ->
            assertEquals(expected = expected, actual = sut.relative(date, TODAY), message = date.toString())
        }
    }
}
