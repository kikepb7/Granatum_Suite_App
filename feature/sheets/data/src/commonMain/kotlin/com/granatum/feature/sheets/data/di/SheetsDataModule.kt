package com.granatum.feature.sheets.data.di

import com.granatum.feature.sheets.data.remote.SheetsRemoteDataSource
import com.granatum.feature.sheets.data.repository.SheetsRepositoryImpl
import com.granatum.feature.sheets.domain.repository.SheetsRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val sheetsDataModule =
    module {
        single { SheetsRemoteDataSource(get()) }
        single { SheetsRepositoryImpl(get()) } bind SheetsRepository::class
    }
