import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import com.granatum.buildlogic.convention.pathToPackageName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Injects per-environment backend configuration into the binary at build time, so no URL ever
 * lives as a literal in the source tree.
 *
 * Pick the environment with `-Pbuildkonfig.flavor=<local|staging|prod|demo>`; it defaults to [LOCAL],
 * the only one that cannot do damage if you forget. Each value resolves in this order: a Gradle
 * property (handy for injecting from CI without touching files), then `local.properties`, then
 * the built-in default.
 *
 * `local` needs no configuration at all — that is what lets a fresh clone run against a local
 * backend. `staging` and `prod` have no defaults on purpose and fail the build when unset: a
 * binary pointing at the wrong server is worse than no binary.
 *
 * The Android emulator reaches the host machine through the 10.0.2.2 alias, which the iOS
 * simulator does not understand — it shares the Mac's network and wants plain localhost. A single
 * shared value therefore breaks one of the two platforms, so the iOS target gets its own default
 * via [BuildKonfigExtension.targetConfigs].
 */
private const val DEFAULT_BASE_URL_HTTP_ANDROID = "http://10.0.2.2:8080/api"
private const val DEFAULT_BASE_URL_HTTP_IOS = "http://localhost:8080/api"

private const val LOCAL = "local"
private const val STAGING = "staging"
private const val PROD = "prod"

/** No server at all: the app talks to an in-memory imitation of the API (core/data DemoMode). */
private const val DEMO = "demo"

private val FLAVORS = listOf(LOCAL, STAGING, PROD, DEMO)

class BuildKonfigConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.codingfeline.buildkonfig")
            }

            val localProperties = gradleLocalProperties(rootDir, rootProject.providers)

            fun resolve(key: String): String? =
                providers.gradleProperty(key).orNull ?: localProperties.getProperty(key)

            // Fails the build rather than falling back to a default, so `staging` and `prod` can
            // never silently inherit the emulator URL.
            fun require(key: String, flavor: String): String =
                resolve(key) ?: throw IllegalStateException(
                    "Missing $key property in local.properties (required by the '$flavor' flavor)"
                )

            fun requireHttps(url: String, flavor: String): String =
                url.takeIf { it.startsWith("https://") } ?: throw IllegalStateException(
                    "The '$flavor' flavor must use an encrypted connection, but got: $url"
                )

            // Also from local.properties, so an Xcode build (whose Gradle call takes no -P) can
            // be switched too, e.g. to the demo: `buildkonfig.flavor=demo`.
            val requestedFlavor = resolve("buildkonfig.flavor")
            if (requestedFlavor != null && requestedFlavor !in FLAVORS) {
                throw IllegalStateException(
                    "Unknown buildkonfig.flavor '$requestedFlavor'. Valid flavors: ${FLAVORS.joinToString()}"
                )
            }
            val selectedFlavor = requestedFlavor ?: LOCAL

            extensions.configure<BuildKonfigExtension> {
                packageName = target.pathToPackageName()

                defaultConfigs {
                    // Through resolve() like every other key, so CI can pass -PAPI_KEY=… without
                    // a local.properties, which is gitignored and never exists on a runner.
                    val apiKey = resolve("API_KEY")
                        ?: throw IllegalStateException(
                            "Missing API_KEY: set it in local.properties or pass -PAPI_KEY=…"
                        )
                    buildConfigField(FieldSpec.Type.STRING, "API_KEY", apiKey)
                    buildConfigField(FieldSpec.Type.STRING, "ENVIRONMENT", selectedFlavor)
                    buildConfigField(
                        FieldSpec.Type.STRING,
                        "BASE_URL_HTTP",
                        resolve("BASE_URL_HTTP") ?: DEFAULT_BASE_URL_HTTP_ANDROID
                    )
                }

                // iOS cannot reach 10.0.2.2. Without this the simulator silently fails to find a
                // backend that Android reaches just fine.
                //
                // These names must be the Kotlin *target* names, not a source-set group: a
                // `create("ios")` here matches no target and is dropped without any warning, which
                // looks like success until you read the generated BuildKonfig.
                val iosBaseUrl = resolve("BASE_URL_HTTP_IOS")
                    ?: resolve("BASE_URL_HTTP")
                    ?: DEFAULT_BASE_URL_HTTP_IOS

                targetConfigs {
                    listOf("iosArm64", "iosX64", "iosSimulatorArm64").forEach { iosTarget ->
                        create(iosTarget) {
                            buildConfigField(FieldSpec.Type.STRING, "BASE_URL_HTTP", iosBaseUrl)
                        }
                    }
                }

                // Only the selected flavor is declared. A `defaultConfigs(name) { }` block runs at
                // configuration time whatever flavor you asked for, so declaring staging and prod
                // unconditionally would demand their keys on every local build — exactly the
                // friction this plugin is meant to remove.
                when (selectedFlavor) {
                    STAGING -> defaultConfigs(STAGING) {
                        buildConfigField(
                            FieldSpec.Type.STRING,
                            "BASE_URL_HTTP",
                            requireHttps(require("BASE_URL_HTTP_STAGING", STAGING), STAGING)
                        )
                    }

                    PROD -> defaultConfigs(PROD) {
                        buildConfigField(
                            FieldSpec.Type.STRING,
                            "BASE_URL_HTTP",
                            requireHttps(require("BASE_URL_HTTP_PROD", PROD), PROD)
                        )
                    }
                }
            }
        }
    }
}
