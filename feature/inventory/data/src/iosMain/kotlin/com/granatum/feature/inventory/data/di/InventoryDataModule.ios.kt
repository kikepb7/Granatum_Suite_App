package com.granatum.feature.inventory.data.di

import com.granatum.feature.inventory.database.DatabaseFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformInventoryDataModule: Module = module {
    single { DatabaseFactory() }
}
