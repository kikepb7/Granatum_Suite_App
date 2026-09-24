package com.granatum.core.data.auth.provider

import com.granatum.core.data.provider.RouteProvider

object AuthRoutes : RouteProvider {

    override val baseUrl = "/auth"

    val LOGIN_ROUTE = route(path = "login")
    val REGISTER_ROUTE = route(path = "register")
    val VERIFY_EMAIL_ROUTE = route(path = "verify")
    val RESEND_VERIFICATION_ROUTE = route(path = "resend-verification")
    val FORGOT_PASSWORD_ROUTE = route(path = "forgot-password")
    val RESET_PASSWORD_ROUTE = route(path = "reset-password")
}