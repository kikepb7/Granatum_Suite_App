package com.granatum.feature.events.domain.di

import com.granatum.feature.events.domain.provider.TodayProvider
import com.granatum.feature.events.domain.usecase.DeleteEventUseCase
import com.granatum.feature.events.domain.usecase.FetchAvailableMaterialsUseCase
import com.granatum.feature.events.domain.usecase.FinishEventUseCase
import com.granatum.feature.events.domain.usecase.ObserveEventUseCase
import com.granatum.feature.events.domain.usecase.ObserveEventsUseCase
import com.granatum.feature.events.domain.usecase.RefreshEventsUseCase
import com.granatum.feature.events.domain.usecase.SaveEventUseCase
import com.granatum.feature.events.domain.usecase.SetEventMaterialUseCase
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import kotlin.time.Clock

val eventsDomainModule = module {
    single<TodayProvider> { TodayProvider { Clock.System.todayIn(TimeZone.currentSystemDefault()) } }
    factoryOf(::ObserveEventsUseCase)
    factoryOf(::ObserveEventUseCase)
    factoryOf(::RefreshEventsUseCase)
    factoryOf(::SaveEventUseCase)
    factoryOf(::DeleteEventUseCase)
    factoryOf(::FinishEventUseCase)
    factoryOf(::SetEventMaterialUseCase)
    factoryOf(::FetchAvailableMaterialsUseCase)
}
