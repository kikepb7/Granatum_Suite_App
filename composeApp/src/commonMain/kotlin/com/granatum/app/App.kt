package com.granatum.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.granatum.core.data.networking.BackendHealthProbe
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.app.navigation.DeepLinkListener
import com.granatum.app.navigation.NavigationRoot
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    val healthProbe: BackendHealthProbe = koinInject()

    // Logs once whether the configured backend is reachable. No-op outside the `local`
    // environment, and never surfaces anything to the user.
    LaunchedEffect(Unit) { healthProbe.check() }

    AppTheme {
        NavigationRoot(navController = navController)
        DeepLinkListener(navController = navController)
    }
}
