package com.granatum.app.di

import com.granatum.core.data.di.coreDataModule
import com.granatum.core.presentation.di.corePresentationModule
import com.granatum.feature.clockin.data.di.clockInDataModule
import com.granatum.feature.clockin.domain.di.clockInDomainModule
import com.granatum.feature.clockin.presentation.di.clockInPresentationModule
import com.granatum.feature.auth.presentation.di.authPresentationModule
import com.granatum.feature.inventory.data.di.inventoryDataModule
import com.granatum.feature.inventory.domain.di.inventoryDomainModule
import com.granatum.feature.inventory.presentation.di.inventoryPresentationModule
import com.granatum.feature.invoicing.data.di.invoicingDataModule
import com.granatum.feature.invoicing.domain.di.invoicingDomainModule
import com.granatum.feature.invoicing.presentation.di.invoicingPresentationModule
import com.granatum.feature.staff.data.di.staffDataModule
import com.granatum.feature.staff.domain.di.staffDomainModule
import com.granatum.feature.staff.presentation.di.staffPresentationModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Add each new feature's data/domain/presentation modules here as you build
 * them out, following the same trio pattern as `inventory`/`clockin` below.
 */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            coreDataModule,
            corePresentationModule,
            appModule,
            inventoryDataModule,
            inventoryDomainModule,
            inventoryPresentationModule,
            clockInDataModule,
            clockInDomainModule,
            clockInPresentationModule,
            invoicingDataModule,
            invoicingDomainModule,
            invoicingPresentationModule,
            staffDataModule,
            staffDomainModule,
            staffPresentationModule,
            authPresentationModule
        )
    }
}
