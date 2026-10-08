package com.granatum.feature.clockin.domain.di

import com.granatum.feature.clockin.domain.usecase.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val clockInDomainModule = module {
    singleOf(::ClockInUseCase)
    singleOf(::ClockOutUseCase)
    singleOf(::StartBreakUseCase)
    singleOf(::EndBreakUseCase)
    singleOf(::ObserveTodayUseCase)
    singleOf(::ObservePendingSyncCountUseCase)
    singleOf(::ObserveMonthUseCase)
    singleOf(::RefreshMonthUseCase)
    singleOf(::ObserveShiftUseCase)
    singleOf(::GetCorrectionsUseCase)
    singleOf(::SyncPendingClockEventsUseCase)
    singleOf(::RequestCorrectionUseCase)
    singleOf(::GetTeamAttendanceUseCase)
    singleOf(::GetPendingCorrectionsUseCase)
    singleOf(::RefreshTeamAttendanceUseCase)
    singleOf(::ApproveCorrectionUseCase)
    singleOf(::RejectCorrectionUseCase)
}
