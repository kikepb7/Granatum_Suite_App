package com.granatum.feature.clockin.presentation.di

import com.granatum.feature.clockin.presentation.clockin.ClockInViewModel
import com.granatum.feature.clockin.presentation.correction.CorrectionViewModel
import com.granatum.feature.clockin.presentation.history.AttendanceHistoryViewModel
import com.granatum.feature.clockin.presentation.shift.ShiftDetailViewModel
import com.granatum.feature.clockin.presentation.team.TeamAttendanceViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Explicit constructors where a parameter has a default (the clock, the time zone): viewModelOf
// would try to resolve it from Koin instead of using the default.
val clockInPresentationModule = module {
    viewModel { ClockInViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { AttendanceHistoryViewModel(get(), get()) }
    viewModel { (shiftKey: String) -> ShiftDetailViewModel(shiftKey, get(), get()) }
    viewModel { (shiftServerId: String) -> CorrectionViewModel(shiftServerId, get(), get()) }
    viewModelOf(::TeamAttendanceViewModel)
}
