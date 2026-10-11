package com.granatum.app.di

import com.granatum.core.data.environment.DemoGateway
import com.granatum.core.demo.DemoGatewayImpl
import org.koin.dsl.module

/** PRE build: the Demo environment exists (specs/010-entornos, research D5). */
val environmentModule = module {
    single<DemoGateway> { DemoGatewayImpl() }
}
