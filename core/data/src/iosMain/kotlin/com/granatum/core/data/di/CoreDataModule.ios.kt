package com.granatum.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.granatum.core.data.auth.createDataStore
import com.granatum.core.data.auth.storage.KeychainSecureStore
import com.granatum.core.data.auth.storage.SecureStore
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

actual val platformCoreDataModule = module {
    single<HttpClientEngine> { Darwin.create() }
    single<DataStore<Preferences>> {
        createDataStore(get())
    }
    single<SecureStore> { KeychainSecureStore(get()) }
}