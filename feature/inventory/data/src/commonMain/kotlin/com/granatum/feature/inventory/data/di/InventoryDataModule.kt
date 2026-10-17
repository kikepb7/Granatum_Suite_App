package com.granatum.feature.inventory.data.di

import com.granatum.core.domain.environment.EnvironmentScopedData
import com.granatum.feature.inventory.data.environment.InventoryEnvironmentData
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.granatum.feature.inventory.data.remote.InventoryRemoteDataSource
import com.granatum.feature.inventory.data.repository.OfflineFirstInventoryRepository
import com.granatum.feature.inventory.database.AppInventoryDatabase
import com.granatum.feature.inventory.database.DatabaseFactory
import com.granatum.feature.inventory.domain.repository.InventoryRepository
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformInventoryDataModule: Module

val inventoryDataModule = module {
    includes(platformInventoryDataModule)

    single {
        get<DatabaseFactory>()
            .create()
            .setDriver(BundledSQLiteDriver())
            // A read cache: older versions (1, spec 006's material in 2, the Spanish change types in 3) are rebuilt from the server.
            .fallbackToDestructiveMigrationFrom(true, 1, 2, 3)
            .build()
    }
    single { get<AppInventoryDatabase>().inventoryDao }
    single { InventoryEnvironmentData(get<AppInventoryDatabase>().maintenanceDao) } bind EnvironmentScopedData::class
    single { InventoryRemoteDataSource(get()) }
    single { OfflineFirstInventoryRepository(get(), get()) } bind InventoryRepository::class
}
