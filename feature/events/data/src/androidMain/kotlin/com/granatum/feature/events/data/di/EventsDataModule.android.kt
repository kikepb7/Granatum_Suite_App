package com.granatum.feature.events.data.di

import com.granatum.feature.events.database.DatabaseFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformEventsDataModule: Module = module {
    single { DatabaseFactory(context = androidContext()) }
}
