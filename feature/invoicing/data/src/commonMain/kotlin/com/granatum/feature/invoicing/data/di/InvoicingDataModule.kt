package com.granatum.feature.invoicing.data.di

import com.granatum.feature.invoicing.data.remote.InvoicingRemoteDataSource
import com.granatum.feature.invoicing.data.repository.KtorInvoicingRepository
import com.granatum.feature.invoicing.domain.repository.InvoicingRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val invoicingDataModule =
    module {
        single { InvoicingRemoteDataSource(get()) }
        single { KtorInvoicingRepository(get()) } bind InvoicingRepository::class
    }
