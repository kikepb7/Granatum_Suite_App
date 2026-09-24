package com.granatum.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.app.navigation.DeepLinkListener
import com.granatum.app.navigation.NavigationRoot
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val navController = rememberNavController()

    AppTheme {
        NavigationRoot(navController = navController)
        DeepLinkListener(navController = navController)
    }
}
