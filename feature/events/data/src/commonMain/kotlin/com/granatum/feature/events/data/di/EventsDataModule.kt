package com.granatum.feature.events.data.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.granatum.core.domain.environment.EnvironmentScopedData
import com.granatum.feature.events.data.environment.EventsEnvironmentData
import com.granatum.feature.events.data.mapper.EventMapper
import com.granatum.feature.events.data.remote.EventsRemoteDataSource
import com.granatum.feature.events.data.repository.EventsRepositoryImpl
import com.granatum.feature.events.database.AppEventsDatabase
import com.granatum.feature.events.database.DatabaseFactory
import com.granatum.feature.events.database.migration.EventsMigrations
import com.granatum.feature.events.domain.repository.EventsRepository
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformEventsDataModule: Module

val eventsDataModule = module {
    includes(platformEventsDataModule)

    single {
        get<DatabaseFactory>()
            .create()
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*EventsMigrations.ALL)
            .build()
    }
    single { get<AppEventsDatabase>().eventsDao }
    single { EventsEnvironmentData(get()) } bind EnvironmentScopedData::class
    single { EventsRemoteDataSource(get()) }
    single { EventMapper() }
    single { EventsRepositoryImpl(get(), get(), get()) } bind EventsRepository::class
}
