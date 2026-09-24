package com.granatum.feature.inventory.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.granatum.feature.inventory.presentation.detail.MaterialDetailRoot
import com.granatum.feature.inventory.presentation.form.MaterialFormRoot
import com.granatum.feature.inventory.presentation.list.MaterialListRoot
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.MaterialCreateRoute
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.MaterialDetailRoute
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.MaterialEditRoute
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.MaterialListRoute
import kotlinx.serialization.Serializable

sealed interface InventoryGraphRoutes {
    @Serializable
    data object MaterialListRoute : InventoryGraphRoutes

    @Serializable
    data class MaterialDetailRoute(val materialId: String) : InventoryGraphRoutes

    @Serializable
    data object MaterialCreateRoute : InventoryGraphRoutes

    @Serializable
    data class MaterialEditRoute(val materialId: String) : InventoryGraphRoutes
}

fun NavGraphBuilder.inventoryGraph(navController: NavHostController) {
    composable<MaterialListRoute> {
        MaterialListRoot(
            onNavigateToDetail = { materialId -> navController.navigate(MaterialDetailRoute(materialId)) },
            onNavigateToCreate = { navController.navigate(MaterialCreateRoute) }
        )
    }
    composable<MaterialDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MaterialDetailRoute>()
        MaterialDetailRoot(
            materialId = route.materialId,
            onNavigateBack = navController::popBackStack,
            onNavigateToEdit = { materialId -> navController.navigate(MaterialEditRoute(materialId)) }
        )
    }
    composable<MaterialCreateRoute> {
        MaterialFormRoot(
            materialId = null,
            onSaved = { navController.popBackStack() },
            onNavigateBack = navController::popBackStack
        )
    }
    composable<MaterialEditRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MaterialEditRoute>()
        MaterialFormRoot(
            materialId = route.materialId,
            onSaved = { navController.popBackStack() },
            onNavigateBack = navController::popBackStack
        )
    }
}
