package com.granatum.feature.clockin.presentation.navigation

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.granatum.feature.clockin.presentation.correction.CorrectionRoot
import com.granatum.feature.clockin.presentation.shift.ShiftDetailRoot
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.CorrectionRoute
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.ShiftDetailRoute
import androidx.navigation.compose.composable
import com.granatum.feature.clockin.presentation.clockin.ClockInRoot
import com.granatum.feature.clockin.presentation.history.AttendanceHistoryRoot
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.ClockInRoute
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.HistoryRoute
import com.granatum.feature.clockin.presentation.navigation.ClockInGraphRoutes.TeamAttendanceRoute
import com.granatum.feature.clockin.presentation.team.TeamAttendanceRoot
import kotlinx.serialization.Serializable

sealed interface ClockInGraphRoutes {
    @Serializable
    data object ClockInRoute : ClockInGraphRoutes

    @Serializable
    data object HistoryRoute : ClockInGraphRoutes

    @Serializable
    data object TeamAttendanceRoute : ClockInGraphRoutes

    /** One shift, by its key: the server id once it exists, the local id before that. */
    @Serializable
    data class ShiftDetailRoute(val shiftKey: String) : ClockInGraphRoutes

    @Serializable
    data class CorrectionRoute(val shiftServerId: String) : ClockInGraphRoutes
}

/**
 * Destinations a role may not use are not registered at all, so they cannot be reached by
 * route either, not merely hidden from the bottom bar. History, and a shift's detail, are always
 * there: every role may read the time register.
 */
fun NavGraphBuilder.clockInGraph(navController: NavController, canClockIn: Boolean, canSeeTeam: Boolean) {
    val openShift: (String) -> Unit = { navController.navigate(ShiftDetailRoute(it)) }
    if (canClockIn) composable<ClockInRoute> { ClockInRoot(onOpenShift = openShift) }
    composable<HistoryRoute> { AttendanceHistoryRoot(onOpenShift = openShift) }
    composable<ShiftDetailRoute> { entry ->
        val route = entry.toRoute<ShiftDetailRoute>()
        val refresh = entry.savedStateHandle.getStateFlow(CORRECTION_SENT, 0).collectAsStateWithLifecycle()
        ShiftDetailRoot(
            shiftKey = route.shiftKey,
            onBack = { navController.popBackStack() },
            onRequestCorrection = { navController.navigate(CorrectionRoute(it)) },
            refreshToken = refresh.value
        )
    }
    composable<CorrectionRoute> { entry ->
        CorrectionRoot(
            shiftServerId = entry.toRoute<CorrectionRoute>().shiftServerId,
            onBack = { navController.popBackStack() },
            onSent = {
                navController.previousBackStackEntry?.savedStateHandle?.let { it[CORRECTION_SENT] = (it.get<Int>(CORRECTION_SENT) ?: 0) + 1 }
                navController.popBackStack()
            }
        )
    }
    if (canSeeTeam) composable<TeamAttendanceRoute> { TeamAttendanceRoot() }
}

private const val CORRECTION_SENT = "correction_sent"
