package com.granatum.core.data.di

import com.granatum.core.data.auth.KtorAuthRepositoryImpl
import com.granatum.core.data.auth.SessionEvents
import com.granatum.core.data.auth.SessionStateHolder
import com.granatum.core.data.auth.storage.LegacySessionCleaner
import com.granatum.core.data.auth.storage.SecureSessionStorage
import com.granatum.core.data.logger.KermitLogger
import com.granatum.core.data.networking.BackendHealthProbe
import com.granatum.core.data.networking.HttpClientFactory
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformCoreDataModule: Module

val coreDataModule = module {
    includes(platformCoreDataModule)
    single<AppLogger> { KermitLogger }
    single {
        HttpClientFactory(get(), get(), get()).create(get())
    }
    singleOf(::BackendHealthProbe)
    singleOf(::KtorAuthRepositoryImpl) bind AuthRepository::class
    singleOf(::LegacySessionCleaner)
    single { SecureSessionStorage(get(), get()) } bind SessionStorage::class
    singleOf(::SessionEvents)
    single { SessionStateHolder(get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}
