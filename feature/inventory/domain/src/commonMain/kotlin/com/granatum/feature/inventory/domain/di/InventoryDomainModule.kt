package com.granatum.feature.inventory.domain.di

import com.granatum.feature.inventory.domain.usecase.GetMaterialDetailUseCase
import com.granatum.feature.inventory.domain.usecase.GetMaterialsUseCase
import com.granatum.feature.inventory.domain.usecase.GetStockHistoryUseCase
import com.granatum.feature.inventory.domain.usecase.RefreshMaterialsUseCase
import com.granatum.feature.inventory.domain.usecase.SaveMaterialUseCase
import com.granatum.feature.inventory.domain.usecase.UpdateMaterialQuantityUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val inventoryDomainModule = module {
    singleOf(::GetMaterialsUseCase)
    singleOf(::RefreshMaterialsUseCase)
    singleOf(::GetMaterialDetailUseCase)
    singleOf(::GetStockHistoryUseCase)
    singleOf(::SaveMaterialUseCase)
    singleOf(::UpdateMaterialQuantityUseCase)
}
