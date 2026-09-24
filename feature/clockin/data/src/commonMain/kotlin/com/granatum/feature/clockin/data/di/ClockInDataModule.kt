package com.granatum.feature.clockin.data.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.granatum.feature.clockin.data.datasource.local.OfflineFirstClockInRepositoryImpl
import com.granatum.feature.clockin.data.datasource.remote.KtorTeamAttendanceRepositoryImpl
import com.granatum.feature.clockin.data.sync.ClockEventSyncManager
import com.granatum.feature.clockin.database.AppClockInDatabase
import com.granatum.feature.clockin.database.DatabaseFactory
import com.granatum.feature.clockin.database.dao.ClockEventDao
import com.granatum.feature.clockin.domain.repository.ClockInRepository
import com.granatum.feature.clockin.domain.repository.TeamAttendanceRepository
import kotlinx.coroutines.CoroutineScope
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformClockInDataModule: Module

val clockInDataModule = module {
    includes(platformClockInDataModule)

    single {
        get<DatabaseFactory>()
            .create()
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single<ClockEventDao> { get<AppClockInDatabase>().clockEventDao }

    single {
        ClockEventSyncManager(
            httpClient = get(),
            dao = get(),
            connectivityObserver = get(),
            logger = get()
        ).also { it.start(scope = get<CoroutineScope>()) }
    }

    single {
        OfflineFirstClockInRepositoryImpl(
            httpClient = get(),
            dao = get(),
            syncManager = get(),
            appScope = get()
        )
    } bind ClockInRepository::class

    single { KtorTeamAttendanceRepositoryImpl(httpClient = get()) } bind TeamAttendanceRepository::class
}
