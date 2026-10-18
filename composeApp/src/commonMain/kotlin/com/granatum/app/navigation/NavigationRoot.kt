package com.granatum.app.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.granatum.app.account.AccountScreenRoot
import com.granatum.core.data.auth.SessionStateHolder
import com.granatum.core.data.environment.EnvironmentRepositoryImpl
import com.granatum.core.designsystem.components.brand.AppBrandSplash
import com.granatum.core.designsystem.components.icons.AppTabIcons
import com.granatum.core.designsystem.components.navigation.AppBottomBar
import com.granatum.core.designsystem.components.topbar.AccountAction
import com.granatum.core.designsystem.components.topbar.LocalAccountAction
import com.granatum.core.designsystem.theme.AppTheme
import com.granatum.core.domain.auth.repository.SessionStorage
import com.granatum.core.designsystem.components.navigation.AppBottomBarItemModel
import com.granatum.core.domain.auth.model.SessionState
import com.granatum.core.domain.environment.EnvironmentRepository
import com.granatum.core.presentation.environment.EnvironmentRibbon
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
import com.granatum.feature.events.presentation.navigation.EventsGraphRoutes
import com.granatum.feature.events.presentation.navigation.EventsGraphRoutes.EventListRoute
import com.granatum.feature.events.presentation.navigation.eventsGraph
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes
import com.granatum.feature.inventory.presentation.navigation.InventoryGraphRoutes.ProductListRoute
import com.granatum.feature.inventory.presentation.navigation.inventoryGraph
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes.InvoiceListRoute
import com.granatum.feature.invoicing.presentation.navigation.invoicingGraph
import com.granatum.feature.staff.presentation.navigation.StaffGraphRoutes
import com.granatum.feature.staff.presentation.navigation.StaffGraphRoutes.StaffListRoute
import com.granatum.feature.staff.presentation.navigation.staffGraph
import com.granatum.feature.sheets.presentation.navigation.SheetsGraphRoutes.GoogleDriveRoute
import com.granatum.feature.sheets.presentation.navigation.sheetsGraph
import granatumsuite.composeapp.generated.resources.Res
import granatumsuite.composeapp.generated.resources.splash_loading
import granatumsuite.composeapp.generated.resources.tab_clock_in
import granatumsuite.composeapp.generated.resources.tab_events
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
    // Starting in Demo signs in by itself: no login screen flashes in between.
    val startingDemo by koinInject<EnvironmentRepositoryImpl>().startingDemo.collectAsStateWithLifecycle()

    Crossfade(targetState = sessionState is SessionState.Loading || startingDemo, label = "splash") { loading ->
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
    // The start screen is the clock when the role may punch in, otherwise the history.
    val startTab = if (RootTab.ClockIn in tabs) RootTab.ClockIn else RootTab.History
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // A screen that belongs to no tab (the account, a shift opened from the history) keeps the
    // tab it was opened from selected, instead of falling back to the first one.
    var lastSelectedIndex by rememberSaveable { mutableStateOf(tabs.indexOf(startTab).coerceAtLeast(0)) }
    val matchedIndex = tabs.indexOfFirst { it.matches(currentRoute) }
    if (matchedIndex >= 0 && matchedIndex != lastSelectedIndex) lastSelectedIndex = matchedIndex
    val selectedIndex = lastSelectedIndex.coerceIn(0, tabs.lastIndex)

    // The account is reached from the top bar of the main screens, so the bottom bar keeps at
    // most six destinations (specs/008-facturacion D11, widened by specs/012-eventos D4).
    val session by koinInject<SessionStorage>().observeSession().collectAsStateWithLifecycle(initialValue = null)
    val initial = session?.email?.firstOrNull()?.uppercase() ?: "?"
    val accountAction = remember(initial, navController) {
        AccountAction(initial = initial, onClick = { navController.navigate(AccountRoute) { launchSingleTop = true } })
    }

    CompositionLocalProvider(LocalAccountAction provides accountAction) {
    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        // PRE build only: which backend this session is using (specs/010-entornos, FR-006).
        val environments = koinInject<EnvironmentRepository>()
        val showRibbon = environments.canSwitch
        if (showRibbon) {
            val environment by environments.current.collectAsStateWithLifecycle()
            EnvironmentRibbon(environment)
        }
        NavHost(
            navController = navController,
            startDestination = startTab.route,
            // The ribbon already covers the status bar: screens must not add it again.
            modifier = Modifier.weight(1f).then(if (showRibbon) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier)
        ) {
            clockInGraph(navController = navController, canClockIn = role.canClockIn, canSeeTeam = role.canSeeTeam)
            if (role.canManageInventory) {
                inventoryGraph(navController = navController)
            }
            if (role.canSeeEvents) {
                eventsGraph(navController = navController, canManage = role.canManageEvents)
            }
            if (role.canManageInvoicing) {
                invoicingGraph(navController = navController)
            }
            // The Google Drive copy is company set-up, for ADMIN only (specs/013-google-sheets).
            if (role == UserRole.ADMIN) {
                sheetsGraph(navController = navController)
            }
            if (role.canManageStaff) {
                staffGraph(navController = navController, onOpenTeamAttendance = { navController.navigate(TeamAttendanceRoute) })
            }
            composable<AccountRoute> {
                AccountScreenRoot(
                    onChangePasswordClick = { navController.navigate(VoluntaryPasswordChangeRoute) },
                    onGoogleDriveClick = { navController.navigate(GoogleDriveRoute) { launchSingleTop = true } },
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
        // A single destination would need no bar; with events every role has at least two.
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
    /** Every inventory screen (product detail, form, categories) keeps the tab selected. */
    Inventory(Res.string.tab_inventory, ProductListRoute, InventoryGraphRoutes::class.qualifiedName),

    /** Every events screen keeps the tab selected: their routes share the graph's prefix. */
    Events(Res.string.tab_events, EventListRoute, EventsGraphRoutes::class.qualifiedName),
    History(Res.string.tab_history, HistoryRoute, HistoryRoute::class.qualifiedName),
    Invoicing(Res.string.tab_invoicing, InvoiceListRoute, InvoicingGraphRoutes::class.qualifiedName),
    Team(Res.string.tab_team, TeamAttendanceRoute, TeamAttendanceRoute::class.qualifiedName),

    /** ADMIN's Team tab: the staff, with the team's working time one tap away (specs/009-personal). */
    Staff(Res.string.tab_team, StaffListRoute, StaffGraphRoutes::class.qualifiedName);

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

    @Composable
    private fun icon() = when (this) {
        ClockIn -> AppTabIcons.Clock
        Inventory -> AppTabIcons.Inventory
        Events -> AppTabIcons.Events
        History -> AppTabIcons.History
        Invoicing -> AppTabIcons.Invoice
        Team, Staff -> AppTabIcons.Team
    }

    companion object {
        /**
         * The tabs a role may use, in bar order (FR-009): Fichajes, Inventario, Eventos, Histórico,
         * Facturación, then the team for the roles that see it.
         */
        fun forRole(role: UserRole): List<RootTab> = buildList {
            if (role.canClockIn) add(ClockIn)
            if (role.canManageInventory) add(Inventory)
            if (role.canSeeEvents) add(Events)
            add(History)
            if (role.canManageInvoicing) add(Invoicing)
            if (role.canManageStaff) add(Staff) else if (role.canSeeTeam) add(Team)
        }
    }
}
