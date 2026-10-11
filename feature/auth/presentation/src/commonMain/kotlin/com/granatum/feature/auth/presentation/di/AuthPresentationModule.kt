package com.granatum.feature.auth.presentation.di

import com.granatum.feature.auth.presentation.login.LoginViewModel
import com.granatum.feature.auth.presentation.password.ChangePasswordMode
import com.granatum.feature.auth.presentation.password.ChangePasswordViewModel
import com.granatum.feature.auth.presentation.signup.OwnerSignUpViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModel { LoginViewModel(get(), get()) }
    viewModelOf(::OwnerSignUpViewModel)
    viewModel { (mode: ChangePasswordMode) -> ChangePasswordViewModel(get(), mode) }
}
