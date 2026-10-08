package com.granatum.feature.staff.domain.di

import com.granatum.feature.staff.domain.StaffUseCases
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val staffDomainModule =
    module {
        singleOf(::StaffUseCases)
    }
