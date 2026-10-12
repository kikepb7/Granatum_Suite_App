package com.granatum.feature.clockin.presentation

import com.granatum.feature.clockin.presentation.common.timeLabel
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TimeLabelTest {

    @Test
    fun padsHoursAndMinutesToTwoDigits() {
        assertEquals("09:05", Instant.parse("2026-03-02T09:05:00Z").timeLabel(TimeZone.UTC))
    }

    @Test
    fun midnightIsZeroZero() {
        assertEquals("00:00", Instant.parse("2026-03-02T00:00:00Z").timeLabel(TimeZone.UTC))
    }

    @Test
    fun appliesTheGivenTimeZone() {
        assertEquals("11:30", Instant.parse("2026-07-01T09:30:00Z").timeLabel(TimeZone.of("Europe/Madrid")))
    }
}
