package com.granatum.core.demo

import com.granatum.core.data.environment.DemoGateway
import com.granatum.core.domain.auth.model.DemoAccount
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine

/**
 * The Demo environment of the PRE build: an in-memory imitation of the Granatum API (same routes,
 * fields, status and error codes as docs/openapi.json and the backend's contracts) seeded with a
 * small flower shop. Its data lives until the app closes.
 */
class DemoGatewayImpl : DemoGateway {

    private val backend = DemoBackend()

    override fun engine(): HttpClientEngine = MockEngine { request -> backend.handle(this, request) }

    override val accounts: List<DemoAccount> = DemoAccountsCatalog.all

    override val defaultAccount: DemoAccount = DemoAccountsCatalog.ADMIN
}
