package com.granatum.core.data.demo

import com.granatum.core.data.BuildKonfig
import com.granatum.core.domain.auth.model.DemoAccount
import com.granatum.core.domain.auth.model.DemoAccounts
import com.granatum.core.domain.auth.model.UserRole
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine

/**
 * The demo build (`-Pbuildkonfig.flavor=demo`): the app talks to [DemoBackend], an in-memory
 * imitation of the Granatum API, instead of the network. Chosen at build time like every other
 * environment, so a local, staging or production binary can never switch into it.
 */
object DemoMode {
    val isEnabled: Boolean get() = BuildKonfig.ENVIRONMENT == "demo"

    /** Test credentials of the demo backend only; they open nothing anywhere else. */
    val ADMIN = DemoAccount(UserRole.ADMIN, "Lucía Ferrer", "admin@demo.granatum.es", "Demo-Admin-2026!")
    val EMPLOYEE = DemoAccount(UserRole.EMPLEADO, "Marcos Vidal", "empleado@demo.granatum.es", "Demo-Empleado-2026!")

    val accounts = DemoAccounts { if (isEnabled) listOf(ADMIN, EMPLOYEE) else emptyList() }

    /** The engine every request goes through in the demo build. State lives until the app closes. */
    fun engine(): HttpClientEngine = DemoBackend().let { backend -> MockEngine { request -> backend.handle(this, request) } }

    /** The engine to use: the platform's real one, or the demo backend in the demo build. */
    fun engineOr(real: () -> HttpClientEngine): HttpClientEngine = if (isEnabled) engine() else real()
}
