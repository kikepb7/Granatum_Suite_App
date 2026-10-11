package com.granatum.buildlogic.convention

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.gradle.api.Project

/**
 * Which app is being built (specs/010-entornos): `pre`, the internal app with the environment
 * selector and the demo, or `pro`, the store app that only knows production.
 *
 * Chosen at build time with `-Pgranatum.build=pre|pro`, or `granatum.build=…` in
 * `local.properties` (handy for Android Studio's Run button), or Xcode's `GRANATUM_BUILD`, which
 * the Xcode build phase passes on. Defaults to `pre`: the one that cannot reach real data by
 * accident, since it starts on the local or PRE backend.
 */
enum class GranatumBuild(val id: String) {
    PRE("pre"),
    PRO("pro");

    companion object {
        fun of(project: Project): GranatumBuild {
            val requested = project.resolveConfig("granatum.build")
            return values().firstOrNull { it.id == requested } ?: when (requested) {
                null -> PRE
                else -> throw IllegalStateException("Unknown granatum.build '$requested'. Valid: pre, pro")
            }
        }
    }
}

/** A Gradle property, then `local.properties`. */
fun Project.resolveConfig(key: String): String? =
    providers.gradleProperty(key).orNull
        ?: gradleLocalProperties(rootDir, rootProject.providers).getProperty(key)
