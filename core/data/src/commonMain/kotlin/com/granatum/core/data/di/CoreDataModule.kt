package com.granatum.core.data.di

import com.granatum.core.data.auth.KtorAuthRepositoryImpl
import com.granatum.core.data.auth.SessionEvents
import com.granatum.core.data.auth.SessionStateHolder
import com.granatum.core.data.auth.storage.LegacySessionCleaner
import com.granatum.core.data.auth.storage.SecureSessionStorage
import com.granatum.core.data.environment.EnvironmentRepositoryImpl
import com.granatum.core.data.logger.KermitLogger
import com.granatum.core.data.networking.BackendHealthProbe
import com.granatum.core.data.networking.HttpClientFactory
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.environment.EnvironmentRepository
import com.granatum.core.domain.logger.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

expect val platformCoreDataModule: Module

val coreDataModule =
    module {
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
        // Loaded before anything else: every route, including the background sync that may start
        // without UI, is built from the active environment (specs/010-entornos, research D3).
        single(createdAtStart = true) {
            EnvironmentRepositoryImpl(
                dataStore = get(),
                authRepository = { get() },
                scopedData = { getAll() },
                demo = getOrNull(),
                logger = get()
            ).also { repository -> runBlocking { repository.load() } }
        } binds arrayOf(EnvironmentRepository::class)
        single { SessionStateHolder(get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    }
