package com.granatum.core.data.auth.provider

import com.granatum.core.data.provider.RouteProvider

/** The auth routes of docs/openapi.json, relative to BASE_URL_HTTP (which ends in /api). */
object AuthRoutes : RouteProvider {

    override val baseUrl = "/auth"

    val LOGIN_ROUTE = route(path = "login")
    val REFRESH_ROUTE = route(path = "refresh")
    val LOGOUT_ROUTE = route(path = "logout")
    val CHANGE_PASSWORD_ROUTE = route(path = "change-password")

    /**
     * Public routes: the credential travels in the body. They never get an `Authorization`
     * header — an expired access token there would only invite a rejection on a route that does
     * not need one.
     */
    val PUBLIC_ROUTES = setOf(LOGIN_ROUTE, REFRESH_ROUTE, LOGOUT_ROUTE)
}
