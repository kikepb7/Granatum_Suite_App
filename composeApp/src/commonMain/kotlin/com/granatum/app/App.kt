package com.granatum.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.granatum.core.data.auth.storage.LegacySessionCleaner
import com.granatum.core.data.auth.storage.SecureSessionStorage
import com.granatum.core.data.demo.DemoAutoLogin
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
    val demoAutoLogin: DemoAutoLogin = koinInject()

    LaunchedEffect(Unit) {
        // Order matters: drop the plaintext session the previous version left behind before
        // reading the secure one, so nothing can observe the old credential in between.
        legacySessionCleaner.clean()
        sessionStorage.load()
        // Demo build only: open already signed in as the administrator. No-op otherwise.
        demoAutoLogin.run()

        // Logs once whether the configured backend is reachable. No-op outside the `local`
        // environment, and never surfaces anything to the user.
        healthProbe.check()
    }

    AppTheme {
        NavigationRoot()
    }
}
