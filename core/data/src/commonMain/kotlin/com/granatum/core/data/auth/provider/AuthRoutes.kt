package com.granatum.core.data.auth.provider

import com.granatum.core.data.provider.RouteProvider

object AuthRoutes : RouteProvider {

    override val baseUrl = "/auth"

    val LOGIN_ROUTE = route(path = "login")
}
