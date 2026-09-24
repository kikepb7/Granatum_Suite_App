package com.granatum.feature.clockin.presentation.navigation

import androidx.navigation.NavGraphBuilder
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
}

fun NavGraphBuilder.clockInGraph() {
    composable<ClockInRoute> { ClockInRoot() }
    composable<HistoryRoute> { AttendanceHistoryRoot() }
    composable<TeamAttendanceRoute> { TeamAttendanceRoot() }
}
