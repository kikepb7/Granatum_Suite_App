package com.granatum.feature.staff.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.granatum.feature.staff.presentation.detail.StaffDetailRoot
import com.granatum.feature.staff.presentation.list.StaffListRoot
import com.granatum.feature.staff.presentation.navigation.StaffGraphRoutes.OnboardingRoute
import com.granatum.feature.staff.presentation.navigation.StaffGraphRoutes.StaffDetailRoute
import com.granatum.feature.staff.presentation.navigation.StaffGraphRoutes.StaffListRoute
import com.granatum.feature.staff.presentation.onboarding.OnboardingRoot
import kotlinx.serialization.Serializable

sealed interface StaffGraphRoutes {
    @Serializable
    data object StaffListRoute : StaffGraphRoutes

    @Serializable
    data object OnboardingRoute : StaffGraphRoutes

    @Serializable
    data class StaffDetailRoute(
        val memberId: String,
    ) : StaffGraphRoutes
}

/** [onOpenTeamAttendance] leads to the team's working-time screen, owned by the clock-in module. */
fun NavGraphBuilder.staffGraph(
    navController: NavHostController,
    onOpenTeamAttendance: () -> Unit,
) {
    composable<StaffListRoute> {
        StaffListRoot(
            onOpenMember = { id -> navController.navigate(StaffDetailRoute(id)) },
            onOnboard = { navController.navigate(OnboardingRoute) },
            onOpenTeamAttendance = onOpenTeamAttendance,
        )
    }
    composable<OnboardingRoute> {
        OnboardingRoot(onNavigateBack = navController::popBackStack, onDone = navController::popBackStack)
    }
    composable<StaffDetailRoute> { entry ->
        StaffDetailRoot(memberId = entry.toRoute<StaffDetailRoute>().memberId, onNavigateBack = navController::popBackStack)
    }
}
