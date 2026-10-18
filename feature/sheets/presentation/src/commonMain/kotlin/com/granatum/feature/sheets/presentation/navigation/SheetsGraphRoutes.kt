package com.granatum.feature.sheets.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.granatum.feature.sheets.presentation.drive.GoogleDriveScreen
import com.granatum.feature.sheets.presentation.navigation.SheetsGraphRoutes.GoogleDriveRoute
import kotlinx.serialization.Serializable

sealed interface SheetsGraphRoutes {
    @Serializable
    data object GoogleDriveRoute : SheetsGraphRoutes
}

/** The «Google Drive» screen, reached from the account menu; only `ADMIN` gets this graph. */
fun NavGraphBuilder.sheetsGraph(navController: NavHostController) {
    composable<GoogleDriveRoute> {
        GoogleDriveScreen(onNavigateBack = navController::popBackStack)
    }
}
