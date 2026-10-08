package com.granatum.feature.staff.presentation.di

import com.granatum.feature.staff.presentation.detail.StaffDetailViewModel
import com.granatum.feature.staff.presentation.list.StaffListViewModel
import com.granatum.feature.staff.presentation.onboarding.OnboardingViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

val staffPresentationModule =
    module {
        viewModelOf(::StaffListViewModel)
        // Start dates are Spanish working days, as the server's "today".
        viewModel {
            OnboardingViewModel(
                get(),
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.of("Europe/Madrid"))
                    .date,
            )
        }
        viewModel { (memberId: String) -> StaffDetailViewModel(memberId, get(), get()) }
    }
