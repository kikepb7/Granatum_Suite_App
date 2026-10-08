package com.granatum.app.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.granatum.app.account.AccountScreenRoot
import com.granatum.core.data.auth.SessionStateHolder
import com.granatum.core.data.demo.DemoAutoLogin
import com.granatum.core.designsystem.components.brand.AppBrandSplash
import com.granatum.core.designsystem.components.navigation.AppBottomBar
import com.granatum.core.designsystem.components.topbar.AccountAction
import com.granatum.core.designsystem.components.topbar.LocalAccountAction
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.designsystem.components.navigation.AppBottomBarItemModel
import com.granatum.core.domain.auth.model.SessionState
import com.granatum.core.domain.auth.model.SignOutReason
import com.granatum.core.domain.auth.model.UserRole
import com.granatum.core.domain.auth.repository.AuthRepository
import com.granatum.feature.auth.presentation.login.LoginScreenRoot
import com.granatum.feature.auth.presentation.signup.OwnerSignUpRoot
import com.granatum.feature.auth.presentation.password.ChangePasswordMode
import com.granatum.feature.auth.presentation.password.ChangePasswordScreenRoot
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.ClockInRoute
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.HistoryRoute
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.TeamAttendanceRoute
import com.granatum.feature.clockin.presentation.navigation.clockInGraph
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.MaterialListRoute
import com.granatum.feature.inventory.presentation.navigation.inventoryGraph
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes.InvoiceListRoute
import com.granatum.feature.invoicing.presentation.navigation.invoicingGraph
import com.granatum.feature.staff.presentation.navigation.StaffGraphRoutes
import com.granatum.feature.staff.presentation.navigation.StaffGraphRoutes.StaffListRoute
import com.granatum.feature.staff.presentation.navigation.staffGraph
import granatumsuite.composeapp.generated.resources.Res
import granatumsuite.composeapp.generated.resources.splash_loading
import granatumsuite.composeapp.generated.resources.tab_clock_in
import granatumsuite.composeapp.generated.resources.tab_history
import granatumsuite.composeapp.generated.resources.tab_inventory
import granatumsuite.composeapp.generated.resources.tab_invoicing
import granatumsuite.composeapp.generated.resources.tab_team
import kotlinx.coroutines.CoroutineScope
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
    // The demo build signs in by itself at start: no login screen flashes in between.
    val demoSigningIn by koinInject<DemoAutoLogin>().pending.collectAsStateWithLifecycle()

    Crossfade(targetState = sessionState is SessionState.Loading || demoSigningIn, label = "splash") { loading ->
        if (loading) Splash() else Gate(sessionState, authRepository, scope)
    }
}

@Composable
private fun Gate(sessionState: SessionState, authRepository: AuthRepository, scope: CoroutineScope) {
    when (val state = sessionState) {
        SessionState.Loading -> Unit
        is SessionState.SignedOut -> SignedOutRoot(reason = state.reason)
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

/** Without a session: sign in, or, only for the business owner, create the account. */
@Composable
private fun SignedOutRoot(reason: SignOutReason?) {
    var signingUp by rememberSaveable { mutableStateOf(false) }
    if (signingUp) {
        OwnerSignUpRoot(onBackToLogin = { signingUp = false })
    } else {
        LoginScreenRoot(signOutReason = reason, onOwnerSignUp = { signingUp = true })
    }
}

/**
 * The brand splash continues the system one while the stored session is read, and is cut the
 * moment the gate knows where to go: it never holds anyone back (brand guide).
 */
@Composable
private fun Splash() {
    AppBrandSplash(contentDescription = stringResource(Res.string.splash_loading))
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

    // The account is reached from the top bar of the main screens, so the bottom bar keeps at
    // most five destinations (specs/008-facturacion, research D11).
    val session by koinInject<SessionStorage>().observeSession().collectAsStateWithLifecycle(initialValue = null)
    val initial = session?.email?.firstOrNull()?.uppercase() ?: "?"
    val accountAction = remember(initial, navController) {
        AccountAction(initial = initial, onClick = { navController.navigate(AccountRoute) { launchSingleTop = true } })
    }

    CompositionLocalProvider(LocalAccountAction provides accountAction) {
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
            if (role.canManageInvoicing) {
                invoicingGraph(navController = navController)
            }
            if (role.canManageStaff) {
                staffGraph(navController = navController, onOpenTeamAttendance = { navController.navigate(TeamAttendanceRoute) })
            }
            composable<AccountRoute> {
                AccountScreenRoot(
                    onChangePasswordClick = { navController.navigate(VoluntaryPasswordChangeRoute) },
                    onNavigateBack = navController::popBackStack
                )
            }
            composable<VoluntaryPasswordChangeRoute> {
                ChangePasswordScreenRoot(
                    mode = ChangePasswordMode.VOLUNTARY,
                    onDismiss = { navController.popBackStack() },
                    onChanged = { navController.popBackStack() }
                )
            }
        }
        // A single destination needs no bar (representative: the record only).
        if (tabs.size > 1) {
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
    }

    // Deep links only resolve inside a session: before signing in nothing is reachable (FR-001).
    DeepLinkListener(navController = navController)
}

private enum class RootTab(val label: StringResource, val route: Any, val qualifiedName: String?) {
    ClockIn(Res.string.tab_clock_in, ClockInRoute, ClockInRoute::class.qualifiedName),
    History(Res.string.tab_history, HistoryRoute, HistoryRoute::class.qualifiedName),
    Inventory(Res.string.tab_inventory, MaterialListRoute, MaterialListRoute::class.qualifiedName),
    Team(Res.string.tab_team, TeamAttendanceRoute, TeamAttendanceRoute::class.qualifiedName),

    /** ADMIN's Team tab: the staff, with the team's working time one tap away (specs/009-personal). */
    Staff(Res.string.tab_team, StaffListRoute, StaffGraphRoutes::class.qualifiedName),
    Invoicing(Res.string.tab_invoicing, InvoiceListRoute, InvoicingGraphRoutes::class.qualifiedName);

    // Every invoicing screen keeps its tab selected: their routes share the graph's prefix.

    fun matches(currentRoute: String?): Boolean {
        if (currentRoute == null) return false
        if (qualifiedName != null && currentRoute.startsWith(qualifiedName)) return true
        // The team's working time is opened from the staff list: Team stays selected for ADMIN.
        val attendance = TeamAttendanceRoute::class.qualifiedName
        return this == Staff && attendance != null && currentRoute.startsWith(attendance)
    }

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
        Team, Staff -> Icons.Default.Groups
        Invoicing -> Icons.Default.Receipt
    }

    companion object {
        /** The tabs a role may use, in bar order (FR-009). The first one is the start screen. */
        fun forRole(role: UserRole): List<RootTab> = buildList {
            if (role.canClockIn) add(ClockIn)
            add(History)
            if (role.canManageInventory) add(Inventory)
            if (role.canManageStaff) add(Staff) else if (role.canSeeTeam) add(Team)
            if (role.canManageInvoicing) add(Invoicing)
        }
    }
}
