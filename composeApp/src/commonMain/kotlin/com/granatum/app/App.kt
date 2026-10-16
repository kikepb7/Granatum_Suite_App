package com.granatum.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.granatum.core.data.networking.StorageHttpClient
import com.granatum.core.data.auth.storage.LegacySessionCleaner
import com.granatum.core.data.auth.storage.SecureSessionStorage
import com.granatum.core.data.environment.EnvironmentRepositoryImpl
import kotlinx.coroutines.flow.first
import com.granatum.core.data.networking.BackendHealthProbe
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.app.navigation.NavigationRoot
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val healthProbe: BackendHealthProbe = koinInject()
    val sessionStorage: SecureSessionStorage = koinInject()
    val legacySessionCleaner: LegacySessionCleaner = koinInject()
    val environments: EnvironmentRepositoryImpl = koinInject()
    val storageHttp: StorageHttpClient = koinInject()

    // Images load through the storage client: never with the backend's token, and through the
    // switching engine, so the Demo environment serves its own (specs/011, FR-006).
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = storageHttp.client)) }
            .build()
    }

    LaunchedEffect(Unit) {
        // Order matters: drop the plaintext session the previous version left behind before
        // reading the secure one, so nothing can observe the old credential in between.
        legacySessionCleaner.clean()
        sessionStorage.load()
        environments.finishStartup(hasSession = sessionStorage.observeSession().first() != null)

        // Logs once whether the configured backend is reachable. No-op outside the `local`
        // environment, and never surfaces anything to the user.
        healthProbe.check()
    }

    AppTheme {
        NavigationRoot()
    }
}
