package com.granatum.feature.inventory.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.granatum.feature.inventory.presentation.category.CategoryListRoot
import com.granatum.feature.inventory.presentation.detail.ProductDetailRoot
import com.granatum.feature.inventory.presentation.form.ProductFormRoot
import com.granatum.feature.inventory.presentation.list.ProductListRoot
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.CategoryListRoute
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.ProductCreateRoute
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.ProductDetailRoute
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.ProductEditRoute
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.ProductListRoute
import kotlinx.serialization.Serializable

sealed interface InventoryGraphRoutes {
    @Serializable
    data object ProductListRoute : InventoryGraphRoutes

    @Serializable
    data class ProductDetailRoute(val productId: String) : InventoryGraphRoutes

    @Serializable
    data object ProductCreateRoute : InventoryGraphRoutes

    @Serializable
    data class ProductEditRoute(val productId: String) : InventoryGraphRoutes

    @Serializable
    data object CategoryListRoute : InventoryGraphRoutes
}

fun NavGraphBuilder.inventoryGraph(navController: NavHostController) {
    composable<ProductListRoute> {
        ProductListRoot(
            onNavigateToDetail = { productId -> navController.navigate(ProductDetailRoute(productId)) },
            onNavigateToCreate = { navController.navigate(ProductCreateRoute) },
            onNavigateToCategories = { navController.navigate(CategoryListRoute) }
        )
    }
    composable<ProductDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ProductDetailRoute>()
        ProductDetailRoot(
            productId = route.productId,
            onNavigateBack = navController::popBackStack,
            onNavigateToEdit = { productId -> navController.navigate(ProductEditRoute(productId)) }
        )
    }
    composable<ProductCreateRoute> {
        ProductFormRoot(
            productId = null,
            onSaved = { navController.popBackStack() },
            onNavigateBack = navController::popBackStack
        )
    }
    composable<ProductEditRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ProductEditRoute>()
        ProductFormRoot(
            productId = route.productId,
            onSaved = { navController.popBackStack() },
            onNavigateBack = navController::popBackStack
        )
    }
    composable<CategoryListRoute> {
        CategoryListRoot(onNavigateBack = navController::popBackStack)
    }
}
