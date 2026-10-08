package com.granatum.feature.invoicing.presentation.di

import com.granatum.feature.invoicing.presentation.company.CompanyViewModel
import com.granatum.feature.invoicing.presentation.detail.InvoiceDetailViewModel
import com.granatum.feature.invoicing.presentation.detail.InvoiceOriginalViewModel
import com.granatum.feature.invoicing.presentation.history.InvoiceHistoryViewModel
import com.granatum.feature.invoicing.presentation.list.InvoiceListViewModel
import com.granatum.feature.invoicing.presentation.quarters.QuartersViewModel
import com.granatum.feature.invoicing.presentation.report.ReportViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

/** Fiscal periods are those of Spain: "today" is the day in Madrid, as for the server. */
private fun today() =
    Clock.System
        .now()
        .toLocalDateTime(TimeZone.of("Europe/Madrid"))
        .date

val invoicingPresentationModule =
    module {
        // Explicit lambdas: the ViewModels take polling parameters with defaults viewModelOf cannot fill.
        viewModel { InvoiceListViewModel(get()) }
        viewModel { (invoiceId: String) -> InvoiceDetailViewModel(invoiceId, get()) }
        viewModel { (invoiceId: String) -> InvoiceOriginalViewModel(invoiceId, get()) }
        viewModel { (invoiceId: String) -> InvoiceHistoryViewModel(invoiceId, get()) }
        viewModel { QuartersViewModel(get(), today().year) }
        viewModel { today().let { ReportViewModel(get(), it.year, it.month.number) } }
        viewModelOf(::CompanyViewModel)
    }
