package com.granatum.feature.events.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.granatum.feature.events.presentation.detail.EventDetailScreen
import com.granatum.feature.events.presentation.form.EventFormScreen
import com.granatum.feature.events.presentation.list.EventListScreen
import com.granatum.feature.events.presentation.navigation.EventsGraphRoutes.EventCreateRoute
import com.granatum.feature.events.presentation.navigation.EventsGraphRoutes.EventDetailRoute
import com.granatum.feature.events.presentation.navigation.EventsGraphRoutes.EventEditRoute
import com.granatum.feature.events.presentation.navigation.EventsGraphRoutes.EventListRoute
import kotlinx.serialization.Serializable

sealed interface EventsGraphRoutes {
    @Serializable
    data object EventListRoute : EventsGraphRoutes

    @Serializable
    data class EventDetailRoute(val eventId: String) : EventsGraphRoutes

    @Serializable
    data object EventCreateRoute : EventsGraphRoutes

    @Serializable
    data class EventEditRoute(val eventId: String) : EventsGraphRoutes
}

/**
 * The events screens (specs/012-eventos). [canManage] is false for REPRESENTANTE, who reads:
 * no create, edit, material or finish controls, matching the backend's rules.
 */
fun NavGraphBuilder.eventsGraph(navController: NavHostController, canManage: Boolean) {
    composable<EventListRoute> {
        EventListScreen(
            canManage = canManage,
            onOpenEvent = { id -> navController.navigate(EventDetailRoute(id)) },
            onCreateEvent = { navController.navigate(EventCreateRoute) }
        )
    }
    composable<EventDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<EventDetailRoute>()
        EventDetailScreen(
            eventId = route.eventId,
            canManage = canManage,
            onNavigateBack = navController::popBackStack,
            onEdit = { id -> navController.navigate(EventEditRoute(id)) }
        )
    }
    composable<EventCreateRoute> {
        EventFormScreen(
            eventId = null,
            // A new event opens straight away, ready for its material.
            onSaved = { id ->
                navController.navigate(EventDetailRoute(id)) { popUpTo<EventCreateRoute> { inclusive = true } }
            },
            onNavigateBack = navController::popBackStack
        )
    }
    composable<EventEditRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<EventEditRoute>()
        EventFormScreen(
            eventId = route.eventId,
            onSaved = { navController.popBackStack() },
            onNavigateBack = navController::popBackStack
        )
    }
}
