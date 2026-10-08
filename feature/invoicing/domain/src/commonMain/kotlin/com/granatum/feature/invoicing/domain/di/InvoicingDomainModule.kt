package com.granatum.feature.invoicing.domain.di

import com.granatum.feature.invoicing.domain.usecase.InvoicingUseCases
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val invoicingDomainModule =
    module {
        singleOf(::InvoicingUseCases)
    }
