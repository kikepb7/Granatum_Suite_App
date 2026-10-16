import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import com.granatum.buildlogic.convention.GranatumBuild
import com.granatum.buildlogic.convention.pathToPackageName
import com.granatum.buildlogic.convention.resolveConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Injects the backend URLs into the binary at build time, so no URL ever lives as a literal in the
 * source tree (specs/010-entornos).
 *
 * - **PRE** (`granatum.build=pre`, the default): the internal app. It carries every environment it
 *   can switch to at run time: LOCAL (the developer's machine, defaulting to the emulator/simulator
 *   host alias), PRE and PRO when their URLs are configured. A fresh clone needs no configuration.
 * - **PRO** (`granatum.build=pro`): the store app. Only the production URL is compiled in, it must
 *   be HTTPS, and the build fails without it: a binary pointing at the wrong server is worse than
 *   no binary. The other fields are empty, so nothing else can be reached.
 *
 * Each value resolves from a Gradle property (CI), then `local.properties`. `BASE_URL_HTTP_STAGING`
 * and `BASE_URL_HTTP_PROD`, the keys of the previous local/staging/prod flavors, still work.
 *
 * The Android emulator reaches the host through 10.0.2.2, which the iOS simulator does not
 * understand (it wants localhost): the local URL gets an iOS value via [BuildKonfigExtension.targetConfigs].
 */
private const val DEFAULT_LOCAL_URL_ANDROID = "http://10.0.2.2:8080/api"
private const val DEFAULT_LOCAL_URL_IOS = "http://localhost:8080/api"

class BuildKonfigConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.codingfeline.buildkonfig")

            val build = GranatumBuild.of(this)

            fun requireHttps(url: String, key: String): String =
                url.takeIf { it.startsWith("https://") }
                    ?: throw IllegalStateException("$key must use an encrypted connection (https://), but got: $url")

            val preUrl = (resolveConfig("BASE_URL_HTTP_PRE") ?: resolveConfig("BASE_URL_HTTP_STAGING"))
                ?.let { requireHttps(it, "BASE_URL_HTTP_PRE") }
            val proUrl = (resolveConfig("BASE_URL_HTTP_PRO") ?: resolveConfig("BASE_URL_HTTP_PROD"))
                ?.let { requireHttps(it, "BASE_URL_HTTP_PRO") }
                ?: if (build == GranatumBuild.PRO) {
                    throw IllegalStateException("Missing BASE_URL_HTTP_PRO (required by granatum.build=pro)")
                } else {
                    null
                }

            extensions.configure<BuildKonfigExtension> {
                packageName = target.pathToPackageName()

                defaultConfigs {
                    // Through resolveConfig() like every other key, so CI can pass -PAPI_KEY=…
                    // without a local.properties, which is gitignored and never exists on a runner.
                    val apiKey = resolveConfig("API_KEY")
                        ?: throw IllegalStateException("Missing API_KEY: set it in local.properties or pass -PAPI_KEY=…")
                    buildConfigField(FieldSpec.Type.STRING, "API_KEY", apiKey)
                    buildConfigField(FieldSpec.Type.STRING, "BUILD", build.id)
                    buildConfigField(FieldSpec.Type.STRING, "BASE_URL_PRO", proUrl.orEmpty())
                    // The store app knows no other server.
                    val internal = build == GranatumBuild.PRE
                    buildConfigField(FieldSpec.Type.STRING, "BASE_URL_PRE", if (internal) preUrl.orEmpty() else "")
                    buildConfigField(
                        FieldSpec.Type.STRING,
                        "BASE_URL_LOCAL",
                        if (internal) resolveConfig("BASE_URL_HTTP") ?: DEFAULT_LOCAL_URL_ANDROID else ""
                    )
                }

                // These names must be the Kotlin *target* names, not a source-set group: a
                // `create("ios")` here matches no target and is dropped without any warning.
                val iosLocalUrl = resolveConfig("BASE_URL_HTTP_IOS") ?: resolveConfig("BASE_URL_HTTP") ?: DEFAULT_LOCAL_URL_IOS
                targetConfigs {
                    listOf("iosArm64", "iosX64", "iosSimulatorArm64").forEach { iosTarget ->
                        create(iosTarget) {
                            buildConfigField(
                                FieldSpec.Type.STRING,
                                "BASE_URL_LOCAL",
                                if (build == GranatumBuild.PRE) iosLocalUrl else ""
                            )
                        }
                    }
                }
            }
        }
    }
}
