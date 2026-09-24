package com.granatum.feature.clockin.data.di

import com.granatum.feature.clockin.data.sync.ConnectivityObserver
import com.granatum.feature.clockin.database.DatabaseFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformClockInDataModule: Module = module {
    single { DatabaseFactory() }
    single { ConnectivityObserver() }
}
