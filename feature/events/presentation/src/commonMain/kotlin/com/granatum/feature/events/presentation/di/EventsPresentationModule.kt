package com.granatum.feature.events.presentation.di

import com.granatum.feature.events.presentation.common.EventDateFormatter
import com.granatum.feature.events.presentation.common.EventUiMapper
import com.granatum.feature.events.presentation.detail.EventDetailViewModel
import com.granatum.feature.events.presentation.form.EventFormViewModel
import com.granatum.feature.events.presentation.list.EventListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val eventsPresentationModule = module {
    factoryOf(::EventDateFormatter)
    factoryOf(::EventUiMapper)
    viewModelOf(::EventListViewModel)
    viewModel { parameters -> EventFormViewModel(parameters.getOrNull(), get(), get(), get(), get(), get()) }
    viewModel { (eventId: String, canManage: Boolean) ->
        EventDetailViewModel(eventId, canManage, get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
}
