package com.granatum.feature.inventory.presentation.di

import com.granatum.feature.inventory.presentation.category.CategoryListViewModel
import com.granatum.feature.inventory.presentation.detail.ProductDetailViewModel
import com.granatum.feature.inventory.presentation.form.ProductFormViewModel
import com.granatum.feature.inventory.presentation.list.ProductListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val inventoryPresentationModule = module {
    viewModelOf(::ProductListViewModel)
    viewModelOf(::CategoryListViewModel)
    viewModel { (productId: String) -> ProductDetailViewModel(productId, get()) }
    viewModel { parameters -> ProductFormViewModel(parameters.getOrNull(), get()) }
}
