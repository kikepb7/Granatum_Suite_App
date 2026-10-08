package com.granatum.feature.staff.data.di

import com.granatum.feature.staff.data.KtorStaffRepository
import com.granatum.feature.staff.domain.StaffRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val staffDataModule =
    module {
        single { KtorStaffRepository(get()) } bind StaffRepository::class
    }
