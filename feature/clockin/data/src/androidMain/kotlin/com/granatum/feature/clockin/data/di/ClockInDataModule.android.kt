package com.granatum.feature.clockin.data.di

import com.granatum.feature.clockin.data.sync.ConnectivityObserver
import com.granatum.feature.clockin.data.sync.scheduleClockSync
import com.granatum.feature.clockin.database.DatabaseFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformClockInDataModule: Module = module {
    single { DatabaseFactory(context = androidContext()) }
    single { ConnectivityObserver(context = androidContext()) }
    single(createdAtStart = true) { scheduleClockSync(context = androidContext()) }
}
