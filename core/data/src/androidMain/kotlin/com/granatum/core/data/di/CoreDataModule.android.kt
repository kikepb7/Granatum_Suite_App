package com.granatum.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.granatum.core.data.auth.createDataStore
import com.granatum.core.data.auth.storage.KeystoreSecureStore
import com.granatum.core.data.auth.storage.SecureStore
import com.granatum.core.data.environment.DemoGateway
import com.granatum.core.data.environment.SwitchingEngine
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformCoreDataModule =
    module {
        // Demo requests go to the demo module when it is linked (PRE build only).
        single<HttpClientEngine> { SwitchingEngine(real = OkHttp.create(), demo = getOrNull<DemoGateway>()?.engine()) }
        single<DataStore<Preferences>> {
            createDataStore(androidContext(), get())
        }
        single<SecureStore> { KeystoreSecureStore(get(), get()) }
    }
