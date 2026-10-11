package com.granatum.core.data.environment

import com.granatum.core.domain.auth.model.DemoAccount
import io.ktor.client.engine.HttpClientEngine

/**
 * What the demo module (`:core:demo`) provides when it is linked, which only the PRE build does
 * (specs/010-entornos, research D5). Without it the Demo environment does not exist.
 */
interface DemoGateway {
    /** Answers every request of the Demo environment, in memory. */
    fun engine(): HttpClientEngine

    val accounts: List<DemoAccount>

    /** The account Demo signs in with when it is selected. */
    val defaultAccount: DemoAccount
}
