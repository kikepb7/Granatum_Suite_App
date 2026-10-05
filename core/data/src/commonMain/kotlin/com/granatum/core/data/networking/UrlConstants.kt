package com.granatum.core.data.networking

import com.granatum.core.data.BuildKonfig

/**
 * The backend the app talks to. The value is injected at build time by
 * `BuildKonfigConventionPlugin` (see build-logic), never written here: pick the environment with
 * `-Pbuildkonfig.flavor=<local|staging|prod>`.
 *
 * This object stays as a thin wrapper so callers keep importing a stable name instead of the
 * generated `BuildKonfig`, and so there is one place to document where the value comes from.
 * It is a `val`, not a `const val`, because a generated value is not a compile-time constant.
 */
object UrlConstants {
    val BASE_URL_HTTP = BuildKonfig.BASE_URL_HTTP
}
