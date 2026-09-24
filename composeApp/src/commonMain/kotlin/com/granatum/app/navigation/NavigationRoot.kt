package com.granatum.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.granatum.core.designsystem.components.navigation.AppBottomBar
import com.granatum.core.designsystem.components.navigation.AppBottomBarItemModel
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.ClockInRoute
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.HistoryRoute
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.TeamAttendanceRoute
import com.granatum.feature.clockin.presentation.navigation.clockInGraph
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.MaterialListRoute
import com.granatum.feature.inventory.presentation.navigation.inventoryGraph
import org.koin.compose.koinInject

/**
 * Role-gated navigation: EMPLEADO only ever sees their own fichaje (button +
 * history); ADMIN/ENCARGADO additionally get inventory and the team
 * attendance panel. There is no login screen in this MVP yet (see
 * `core/data`'s ready-made `AuthRepository`/`SessionStorage` plumbing for
 * that) — with no session, we fall back to the least-privileged EMPLEADO
 * tab set rather than showing management screens by default.
 */
@Composable
fun NavigationRoot(navController: NavHostController) {
    val sessionStorage = koinInject<SessionStorage>()
    val authInfo by sessionStorage.observeAuthInfo().collectAsStateWithLifecycle(initialValue = null)
    val role = authInfo?.user?.role ?: UserRole.EMPLEADO

    val tabs = buildList {
        add(RootTab.ClockIn)
        add(RootTab.History)
        if (role.canManageTeam) {
            add(RootTab.Inventory)
            add(RootTab.Team)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedIndex = tabs.indexOfFirst { it.matchesCurrentRoute(backStackEntry?.destination?.route) }
        .coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = ClockInRoute,
            modifier = Modifier.weight(1f)
        ) {
            clockInGraph()
            if (role.canManageTeam) {
                inventoryGraph(navController = navController)
            }
        }

        AppBottomBar(
            items = tabs.map { it.toBottomBarItem() },
            selectedIndex = selectedIndex,
            onItemClick = { index ->
                navController.navigate(tabs[index].route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

private sealed class RootTab(val label: String, val route: Any, val routeQualifiedName: String?) {
    data object ClockIn : RootTab(label = "Fichaje", route = ClockInRoute, routeQualifiedName = ClockInRoute::class.qualifiedName)
    data object History : RootTab(label = "Historial", route = HistoryRoute, routeQualifiedName = HistoryRoute::class.qualifiedName)
    data object Inventory : RootTab(label = "Inventario", route = MaterialListRoute, routeQualifiedName = MaterialListRoute::class.qualifiedName)
    data object Team : RootTab(label = "Equipo", route = TeamAttendanceRoute, routeQualifiedName = TeamAttendanceRoute::class.qualifiedName)

    fun matchesCurrentRoute(currentRoute: String?): Boolean =
        currentRoute != null && routeQualifiedName != null && currentRoute.startsWith(routeQualifiedName)

    fun toBottomBarItem(): AppBottomBarItemModel = AppBottomBarItemModel(
        label = label,
        icon = { Icon(imageVector = icon(), contentDescription = label) }
    )

    private fun icon() = when (this) {
        ClockIn -> Icons.Default.Home
        History -> Icons.Default.DateRange
        Inventory -> Icons.Default.Inventory
        Team -> Icons.Default.Groups
    }
}
