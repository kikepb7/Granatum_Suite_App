package com.granatum.app.di

import com.granatum.app.account.AccountViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    viewModelOf(::AccountViewModel)
}
