package com.granatum.feature.clockin.domain.di

import com.granatum.feature.clockin.domain.usecase.ApproveCorrectionUseCase
import com.granatum.feature.clockin.domain.usecase.ClockInUseCase
import com.granatum.feature.clockin.domain.usecase.ClockOutUseCase
import com.granatum.feature.clockin.domain.usecase.EndBreakUseCase
import com.granatum.feature.clockin.domain.usecase.GetAttendanceHistoryUseCase
import com.granatum.feature.clockin.domain.usecase.GetPendingCorrectionsUseCase
import com.granatum.feature.clockin.domain.usecase.GetTeamAttendanceUseCase
import com.granatum.feature.clockin.domain.usecase.ObservePendingSyncCountUseCase
import com.granatum.feature.clockin.domain.usecase.ObserveShiftStatusUseCase
import com.granatum.feature.clockin.domain.usecase.ObserveTodayEventsUseCase
import com.granatum.feature.clockin.domain.usecase.RefreshTeamAttendanceUseCase
import com.granatum.feature.clockin.domain.usecase.RejectCorrectionUseCase
import com.granatum.feature.clockin.domain.usecase.RequestCorrectionUseCase
import com.granatum.feature.clockin.domain.usecase.StartBreakUseCase
import com.granatum.feature.clockin.domain.usecase.SyncPendingClockEventsUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val clockInDomainModule = module {
    singleOf(::ClockInUseCase)
    singleOf(::ClockOutUseCase)
    singleOf(::StartBreakUseCase)
    singleOf(::EndBreakUseCase)
    singleOf(::ObserveShiftStatusUseCase)
    singleOf(::ObserveTodayEventsUseCase)
    singleOf(::ObservePendingSyncCountUseCase)
    singleOf(::GetAttendanceHistoryUseCase)
    singleOf(::SyncPendingClockEventsUseCase)
    singleOf(::RequestCorrectionUseCase)
    singleOf(::GetTeamAttendanceUseCase)
    singleOf(::GetPendingCorrectionsUseCase)
    singleOf(::RefreshTeamAttendanceUseCase)
    singleOf(::ApproveCorrectionUseCase)
    singleOf(::RejectCorrectionUseCase)
}
