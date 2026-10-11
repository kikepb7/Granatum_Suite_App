package com.granatum.core.data.networking

import com.granatum.core.data.environment.ActiveEnvironment

/**
 * The backend the app talks to: the active environment's URL (specs/010-entornos). The URLs are
 * injected at build time by `BuildKonfigConventionPlugin`, never written here; a PRO build has only
 * production's. Read on every call, because the PRE build can switch environment at run time.
 */
object UrlConstants {
    val BASE_URL_HTTP: String get() = ActiveEnvironment.baseUrl
}
