package com.granatum.feature.clockin.data.di

import com.granatum.core.domain.environment.EnvironmentScopedData
import com.granatum.feature.clockin.data.environment.ClockInEnvironmentData
import com.granatum.feature.clockin.data.sync.ConnectivityObserver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.granatum.feature.clockin.data.datasource.local.OfflineFirstClockInRepositoryImpl
import com.granatum.feature.clockin.data.datasource.remote.KtorTeamAttendanceRepositoryImpl
import com.granatum.feature.clockin.data.sync.ShiftSyncEngine
import com.granatum.feature.clockin.data.remote.ShiftRemoteDataSource
import com.granatum.feature.clockin.database.dao.ShiftDao
import com.granatum.feature.clockin.database.dao.ServerShiftDao
import com.granatum.feature.clockin.database.migration.Migration2To3
import com.granatum.feature.clockin.database.migration.Migration3To4
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
            .addMigrations(Migration2To3, Migration3To4)
            .build()
    }
    single<ClockEventDao> { get<AppClockInDatabase>().clockEventDao }
    single<ShiftDao> { get<AppClockInDatabase>().shiftDao }
    single<ServerShiftDao> { get<AppClockInDatabase>().serverShiftDao }
    single { ClockInEnvironmentData(get<AppClockInDatabase>().maintenanceDao) } bind EnvironmentScopedData::class
    single { ShiftRemoteDataSource(httpClient = get()) }

    single {
        ShiftSyncEngine(
            remote = get(),
            eventDao = get(),
            shiftDao = get(),
            serverShiftDao = get(),
            sessionStorage = get(),
            connectivity = get<ConnectivityObserver>().observe(),
            logger = get()
        ).also { it.start(scope = get<CoroutineScope>()) }
    }

    single {
        OfflineFirstClockInRepositoryImpl(
            eventDao = get(),
            shiftDao = get(),
            serverShiftDao = get(),
            remote = get(),
            sessionStorage = get(),
            syncEngine = get(),
            appScope = get()
        )
    } bind ClockInRepository::class

    single { KtorTeamAttendanceRepositoryImpl(httpClient = get()) } bind TeamAttendanceRepository::class
}
