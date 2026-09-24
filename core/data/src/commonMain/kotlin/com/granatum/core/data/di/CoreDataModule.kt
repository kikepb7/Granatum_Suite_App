package com.granatum.core.data.di

import com.granatum.core.data.auth.KtorAuthRepositoryImpl
import com.granatum.core.data.auth.storage.DataStoreSessionStorage
import com.granatum.core.data.logger.KermitLogger
import com.granatum.core.data.networking.HttpClientFactory
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.domain.logger.AppLogger
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformCoreDataModule: Module

val coreDataModule = module {
    includes(platformCoreDataModule)
    single<AppLogger> { KermitLogger }
    single {
        HttpClientFactory(get(), get()).create(get())
    }
    singleOf(::KtorAuthRepositoryImpl) bind AuthRepository::class
    singleOf(::DataStoreSessionStorage) bind SessionStorage::class
}