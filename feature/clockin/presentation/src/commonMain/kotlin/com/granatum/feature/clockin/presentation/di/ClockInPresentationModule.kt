package com.granatum.feature.clockin.presentation.di

import com.granatum.feature.clockin.presentation.clockin.ClockInViewModel
import com.granatum.feature.clockin.presentation.history.AttendanceHistoryViewModel
import com.granatum.feature.clockin.presentation.team.TeamAttendanceViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val clockInPresentationModule = module {
    viewModelOf(::ClockInViewModel)
    viewModelOf(::AttendanceHistoryViewModel)
    viewModelOf(::TeamAttendanceViewModel)
}
