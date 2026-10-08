package com.granatum.feature.inventory.domain.di

import com.granatum.feature.inventory.domain.usecase.InventoryUseCases
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val inventoryDomainModule = module {
    singleOf(::InventoryUseCases)
}
