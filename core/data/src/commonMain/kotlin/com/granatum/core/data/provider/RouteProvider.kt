package com.granatum.core.data.provider

interface RouteProvider {
    val baseUrl: String

    fun route(path: String) = "$baseUrl/$path"
}