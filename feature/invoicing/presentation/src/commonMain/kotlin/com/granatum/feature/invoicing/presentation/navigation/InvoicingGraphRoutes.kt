package com.granatum.feature.invoicing.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.granatum.feature.invoicing.presentation.company.CompanyRoot
import com.granatum.feature.invoicing.presentation.detail.InvoiceDetailRoot
import com.granatum.feature.invoicing.presentation.detail.InvoiceOriginalRoot
import com.granatum.feature.invoicing.presentation.history.InvoiceHistoryRoot
import com.granatum.feature.invoicing.presentation.list.InvoiceListRoot
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes.CompanyRoute
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes.InvoiceDetailRoute
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes.InvoiceHistoryRoute
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes.InvoiceListRoute
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes.InvoiceOriginalRoute
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes.QuartersRoute
import com.granatum.feature.invoicing.presentation.navigation.InvoicingGraphRoutes.ReportRoute
import com.granatum.feature.invoicing.presentation.quarters.QuartersRoot
import com.granatum.feature.invoicing.presentation.report.ReportRoot
import kotlinx.serialization.Serializable

/** Each screen is its own destination, so new invoicing features slot in beside them (FR-018). */
sealed interface InvoicingGraphRoutes {
    @Serializable
    data object InvoiceListRoute : InvoicingGraphRoutes

    @Serializable
    data class InvoiceDetailRoute(
        val invoiceId: String,
    ) : InvoicingGraphRoutes

    @Serializable
    data class InvoiceOriginalRoute(
        val invoiceId: String,
    ) : InvoicingGraphRoutes

    @Serializable
    data class InvoiceHistoryRoute(
        val invoiceId: String,
    ) : InvoicingGraphRoutes

    @Serializable
    data object QuartersRoute : InvoicingGraphRoutes

    @Serializable
    data object ReportRoute : InvoicingGraphRoutes

    @Serializable
    data object CompanyRoute : InvoicingGraphRoutes
}

fun NavGraphBuilder.invoicingGraph(navController: NavHostController) {
    composable<InvoiceListRoute> {
        InvoiceListRoot(
            onOpenInvoice = { id -> navController.navigate(InvoiceDetailRoute(id)) },
            onOpenQuarters = { navController.navigate(QuartersRoute) },
            onOpenReports = { navController.navigate(ReportRoute) },
            onOpenCompany = { navController.navigate(CompanyRoute) },
        )
    }
    composable<InvoiceDetailRoute> { entry ->
        val route = entry.toRoute<InvoiceDetailRoute>()
        InvoiceDetailRoot(
            invoiceId = route.invoiceId,
            onNavigateBack = navController::popBackStack,
            onOpenOriginal = { navController.navigate(InvoiceOriginalRoute(route.invoiceId)) },
            onOpenHistory = { navController.navigate(InvoiceHistoryRoute(route.invoiceId)) },
        )
    }
    composable<InvoiceOriginalRoute> { entry ->
        InvoiceOriginalRoot(invoiceId = entry.toRoute<InvoiceOriginalRoute>().invoiceId, onNavigateBack = navController::popBackStack)
    }
    composable<InvoiceHistoryRoute> { entry ->
        InvoiceHistoryRoot(invoiceId = entry.toRoute<InvoiceHistoryRoute>().invoiceId, onNavigateBack = navController::popBackStack)
    }
    composable<QuartersRoute> { QuartersRoot(onNavigateBack = navController::popBackStack) }
    composable<ReportRoute> { ReportRoot(onNavigateBack = navController::popBackStack) }
    composable<CompanyRoute> { CompanyRoot(onNavigateBack = navController::popBackStack) }
}
