package com.granatum.feature.events.presentation.common

import com.granatum.core.presentation.util.UiText
import granatumsuite.feature.events.presentation.generated.resources.Res
import granatumsuite.feature.events.presentation.generated.resources.when_days_ago
import granatumsuite.feature.events.presentation.generated.resources.when_in_days
import granatumsuite.feature.events.presentation.generated.resources.when_today
import granatumsuite.feature.events.presentation.generated.resources.when_tomorrow
import granatumsuite.feature.events.presentation.generated.resources.when_yesterday
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/**
 * Event dates as a Spanish reader writes them (`sáb, 13 jun 2027`) and how far they are from
 * today (`Mañana`, `En 5 días`). Kept out of the composables and the ViewModels (android-patterns,
 * section 9). The app is in Spanish only, so the names are written here rather than taken from a
 * locale the device may not have.
 */
internal class EventDateFormatter {

    /** `sáb, 13 jun 2027` */
    fun long(date: LocalDate): String = "${weekday(date.dayOfWeek)}, ${date.day} ${month(date)} ${date.year}"

    /** `13/06/2027` */
    fun short(date: LocalDate): String =
        "${date.day.toString().padStart(2, '0')}/${date.month.number.toString().padStart(2, '0')}/${date.year}"

    /** `13`, for the calendar badge of a card. */
    fun day(date: LocalDate): String = date.day.toString()

    /** `JUN`, for the calendar badge of a card. */
    fun monthBadge(date: LocalDate): String = month(date).uppercase()

    fun relative(date: LocalDate, today: LocalDate): UiText {
        val days = date.toEpochDays() - today.toEpochDays()
        return when {
            days == 0L -> UiText.Resource(Res.string.when_today)
            days == 1L -> UiText.Resource(Res.string.when_tomorrow)
            days > 1L -> UiText.Resource(Res.string.when_in_days, arrayOf(days.toInt()))
            days == -1L -> UiText.Resource(Res.string.when_yesterday)
            else -> UiText.Resource(Res.string.when_days_ago, arrayOf((-days).toInt()))
        }
    }

    private fun month(date: LocalDate) = MONTHS[date.month.number - 1]

    private fun weekday(day: DayOfWeek) = WEEKDAYS[day.ordinal]

    private companion object {
        val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")

        /** In [DayOfWeek] order: Monday first. */
        val WEEKDAYS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
    }
}
