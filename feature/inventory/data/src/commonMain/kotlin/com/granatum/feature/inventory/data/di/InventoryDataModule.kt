package com.granatum.feature.inventory.data.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.granatum.feature.inventory.data.datasource.local.OfflineFirstMaterialRepositoryImpl
import com.granatum.feature.inventory.database.DatabaseFactory
import com.granatum.feature.inventory.domain.repository.MaterialRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformInventoryDataModule: Module

val inventoryDataModule = module {
    includes(platformInventoryDataModule)

    single {
        get<DatabaseFactory>()
            .create()
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    singleOf(::OfflineFirstMaterialRepositoryImpl) bind MaterialRepository::class
}
