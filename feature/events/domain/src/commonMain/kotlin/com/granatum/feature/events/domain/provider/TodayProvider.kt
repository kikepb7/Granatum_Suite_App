package com.granatum.feature.events.domain.provider

import kotlinx.datetime.LocalDate

/** Today's date where the device is: what "an event in the past" is measured against. */
fun interface TodayProvider {
    fun today(): LocalDate
}
