plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.hot.reload) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false

    // Applied here rather than only declared: the root is the one place that can see every
    // module, so it aggregates their coverage and enforces the ratchet on the total. The
    // per-module setup lives in QualityConventionPlugin (build-logic).
    //
    // This cannot be a convention plugin applied to the root: loading any build-logic plugin
    // here puts the whole build-logic jar on the root classpath, and every module's
    // `alias(libs.plugins.convention.*)` request then fails as "already on the classpath with
    // an unknown version".
    alias(libs.plugins.kover)
}

// Only modules that actually apply Kover can be aggregated. Container projects such as :core
// or :feature have no sources and no Kover.
subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlinx.kover") {
        rootProject.dependencies.add("kover", this@subprojects)
    }
}

// The coverage ratchet from constitution principle VIII: may be raised in any PR, lowering it
// needs an amendment. Required rather than defaulted, so deleting the line fails the build
// instead of silently removing the gate.
val coverageFloor = providers.gradleProperty("granatum.coverage.minLine").orNull?.toIntOrNull()
    ?: throw IllegalStateException(
        "Missing granatum.coverage.minLine in gradle.properties: it is the coverage ratchet " +
            "from constitution principle VIII."
    )

// Coverage is aggregated on Kover's total variant. It runs each module's unit tests in both the
// debug and release variants — the same commonTest twice — which doubles that stage's time for
// no extra coverage. Merging only the "debug" variants into the root was tried and fails:
// :composeApp is an Android *application*, and its variant does not resolve against a custom
// root variant. With a single test today the cost is nil; revisit once real tests exist.
kover {
    reports {
        verify {
            rule {
                minBound(coverageFloor)
            }
        }
    }
}
