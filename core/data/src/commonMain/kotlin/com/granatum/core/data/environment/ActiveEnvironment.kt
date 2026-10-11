package com.granatum.core.data.environment

import com.granatum.core.data.BuildKonfig
import com.granatum.core.domain.environment.AppEnvironment
import kotlin.concurrent.Volatile

/**
 * The environments compiled into this build and the one in use (specs/010-entornos, research D3).
 *
 * The URLs come from `BuildKonfigConventionPlugin`: a PRO build has only the production one, so
 * nothing in it can point anywhere else. The active environment is process-wide because every
 * route is built from it (`constructRoute`), including the background sync that runs without UI.
 */
object ActiveEnvironment {

    val isProBuild: Boolean get() = BuildKonfig.BUILD == "pro"

    /** Never resolved over the network: the demo engine answers before anything is sent. */
    const val DEMO_BASE_URL = "https://demo.granatum.invalid/api"

    fun baseUrlOf(environment: AppEnvironment): String = when (environment) {
        AppEnvironment.LOCAL -> BuildKonfig.BASE_URL_LOCAL
        AppEnvironment.PRE -> BuildKonfig.BASE_URL_PRE
        AppEnvironment.PRO -> BuildKonfig.BASE_URL_PRO
        AppEnvironment.DEMO -> DEMO_BASE_URL
    }

    /** What this build can use, in display order; [demoAvailable] when the demo module is linked. */
    fun available(demoAvailable: Boolean): List<AppEnvironment> =
        availableFor(isProBuild, demoAvailable) { baseUrlOf(it).isNotEmpty() }

    /** The rule behind [available]: PRO alone in the store app; otherwise what is configured. */
    fun availableFor(proBuild: Boolean, demoAvailable: Boolean, configured: (AppEnvironment) -> Boolean): List<AppEnvironment> =
        if (proBuild) {
            listOf(AppEnvironment.PRO)
        } else {
            AppEnvironment.entries.filter { environment ->
                if (environment == AppEnvironment.DEMO) demoAvailable else configured(environment)
            }
        }

    /** Where a fresh install starts: production in the store app, PRE (or the local machine) otherwise. */
    fun default(available: List<AppEnvironment>): AppEnvironment = when {
        available == listOf(AppEnvironment.PRO) -> AppEnvironment.PRO
        AppEnvironment.PRE in available -> AppEnvironment.PRE
        AppEnvironment.LOCAL in available -> AppEnvironment.LOCAL
        else -> available.first()
    }

    @Volatile
    var current: AppEnvironment = if (isProBuild) AppEnvironment.PRO else AppEnvironment.LOCAL
        internal set

    val baseUrl: String get() = baseUrlOf(current)
}
