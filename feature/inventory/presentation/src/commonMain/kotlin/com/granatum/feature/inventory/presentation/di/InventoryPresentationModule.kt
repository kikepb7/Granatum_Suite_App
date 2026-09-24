package com.granatum.feature.inventory.presentation.di

import com.granatum.feature.inventory.presentation.detail.MaterialDetailViewModel
import com.granatum.feature.inventory.presentation.form.MaterialFormViewModel
import com.granatum.feature.inventory.presentation.list.MaterialListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val inventoryPresentationModule = module {
    viewModelOf(::MaterialListViewModel)
    viewModel { parameters ->
        MaterialDetailViewModel(
            materialId = parameters.get(),
            getMaterialDetailUseCase = get(),
            getStockHistoryUseCase = get(),
            updateMaterialQuantityUseCase = get()
        )
    }
    viewModel { parameters ->
        MaterialFormViewModel(
            materialId = parameters.getOrNull(),
            getMaterialDetailUseCase = get(),
            saveMaterialUseCase = get()
        )
    }
}
