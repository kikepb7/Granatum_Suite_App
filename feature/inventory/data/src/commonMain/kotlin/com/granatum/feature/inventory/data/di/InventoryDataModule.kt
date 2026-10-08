package com.granatum.feature.inventory.data.di

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
            // Version 1 was a cache that never held real data: rebuilt from the server (research D2).
            .fallbackToDestructiveMigrationFrom(true, 1)
            .build()
    }
    single { get<AppInventoryDatabase>().inventoryDao }
    single { InventoryRemoteDataSource(get()) }
    single { OfflineFirstInventoryRepository(get(), get()) } bind InventoryRepository::class
}
