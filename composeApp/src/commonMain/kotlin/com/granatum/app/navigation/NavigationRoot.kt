package com.granatum.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.granatum.app.account.AccountScreenRoot
import com.granatum.core.data.auth.SessionStateHolder
import com.granatum.core.designsystem.components.brand.AppBrandLogo
import com.granatum.core.designsystem.components.navigation.AppBottomBar
import com.granatum.core.designsystem.components.navigation.AppBottomBarItemModel
import com.granatum.core.domain.auth.model.SessionState
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.feature.auth.presentation.login.LoginScreenRoot
import com.granatum.feature.auth.presentation.password.ChangePasswordMode
import com.granatum.feature.auth.presentation.password.ChangePasswordScreenRoot
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.ClockInRoute
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.HistoryRoute
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.TeamAttendanceRoute
import com.granatum.feature.clockin.presentation.navigation.clockInGraph
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.MaterialListRoute
import com.granatum.feature.inventory.presentation.navigation.inventoryGraph
import granatumsuite.composeapp.generated.resources.Res
import granatumsuite.composeapp.generated.resources.splash_loading
import granatumsuite.composeapp.generated.resources.tab_account
import granatumsuite.composeapp.generated.resources.tab_clock_in
import granatumsuite.composeapp.generated.resources.tab_history
import granatumsuite.composeapp.generated.resources.tab_inventory
import granatumsuite.composeapp.generated.resources.tab_team
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The gate. What the app shows depends on the session state alone (spec 004, research D8):
 * without a session there is only the login screen, a session pending a password change sees
 * only that change, and an active session sees the tabs its role allows — never a default role.
 */
@Composable
fun NavigationRoot() {
    val sessionStateHolder = koinInject<SessionStateHolder>()
    val authRepository = koinInject<AuthRepository>()
    val scope = rememberCoroutineScope()
    val sessionState by sessionStateHolder.state.collectAsStateWithLifecycle()

    when (val state = sessionState) {
        SessionState.Loading -> Splash()
        is SessionState.SignedOut -> LoginScreenRoot(signOutReason = state.reason)
        SessionState.PasswordChangeRequired -> ChangePasswordScreenRoot(
            mode = ChangePasswordMode.MANDATORY,
            // The only other thing the server allows this session to do.
            onDismiss = { scope.launch { authRepository.logout() } },
            // Nothing to do: the new session no longer needs a change and the gate moves on.
            onChanged = {}
        )
        // A fresh nav controller per role, so a new sign-in never inherits the last back stack.
        is SessionState.Active -> key(state.role) { SignedInRoot(role = state.role) }
    }
}

@Composable
private fun Splash() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        val loading = stringResource(Res.string.splash_loading)
        AppBrandLogo(modifier = Modifier.size(96.dp).semantics { contentDescription = loading })
    }
}

@Serializable
private data object AccountRoute

@Serializable
private data object VoluntaryPasswordChangeRoute

@Composable
private fun SignedInRoot(role: UserRole) {
    val navController = rememberNavController()
    val tabs = RootTab.forRole(role)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedIndex = tabs.indexOfFirst { it.matches(currentRoute) }.coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = tabs.first().route,
            modifier = Modifier.weight(1f)
        ) {
            clockInGraph(navController = navController, canClockIn = role.canClockIn, canSeeTeam = role.canSeeTeam)
            if (role.canManageInventory) {
                inventoryGraph(navController = navController)
            }
            composable<AccountRoute> {
                AccountScreenRoot(onChangePasswordClick = { navController.navigate(VoluntaryPasswordChangeRoute) })
            }
            composable<VoluntaryPasswordChangeRoute> {
                ChangePasswordScreenRoot(
                    mode = ChangePasswordMode.VOLUNTARY,
                    onDismiss = { navController.popBackStack() },
                    onChanged = { navController.popBackStack() }
                )
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

    // Deep links only resolve inside a session: before signing in nothing is reachable (FR-001).
    DeepLinkListener(navController = navController)
}

private enum class RootTab(val label: StringResource, val route: Any, val qualifiedName: String?) {
    ClockIn(Res.string.tab_clock_in, ClockInRoute, ClockInRoute::class.qualifiedName),
    History(Res.string.tab_history, HistoryRoute, HistoryRoute::class.qualifiedName),
    Inventory(Res.string.tab_inventory, MaterialListRoute, MaterialListRoute::class.qualifiedName),
    Team(Res.string.tab_team, TeamAttendanceRoute, TeamAttendanceRoute::class.qualifiedName),
    Account(Res.string.tab_account, AccountRoute, AccountRoute::class.qualifiedName);

    fun matches(currentRoute: String?): Boolean =
        currentRoute != null && qualifiedName != null && currentRoute.startsWith(qualifiedName)

    @Composable
    fun toBottomBarItem(): AppBottomBarItemModel {
        val text = stringResource(label)
        return AppBottomBarItemModel(
            label = text,
            icon = { Icon(imageVector = icon(), contentDescription = text) }
        )
    }

    private fun icon() = when (this) {
        ClockIn -> Icons.Default.Home
        History -> Icons.Default.DateRange
        Inventory -> Icons.Default.Inventory
        Team -> Icons.Default.Groups
        Account -> Icons.Default.AccountCircle
    }

    companion object {
        /** The tabs a role may use, in bar order (FR-009). The first one is the start screen. */
        fun forRole(role: UserRole): List<RootTab> = buildList {
            if (role.canClockIn) add(ClockIn)
            add(History)
            if (role.canManageInventory) add(Inventory)
            if (role.canSeeTeam) add(Team)
            add(Account)
        }
    }
}
