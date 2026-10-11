import com.android.build.api.dsl.ApplicationExtension
import com.granatum.buildlogic.convention.configureKotlinAndroid
import com.granatum.buildlogic.convention.libs
import com.granatum.buildlogic.convention.GranatumBuild
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
            }

            extensions.configure<ApplicationExtension> {
                namespace = "com.granatum.app"

                defaultConfig {
                    applicationId = libs.findVersion("projectApplicationId").get().toString()
                    targetSdk = libs.findVersion("projectTargetSdkVersion").get().toString().toInt()
                    versionCode = libs.findVersion("projectVersionCode").get().toString().toInt()
                    versionName = libs.findVersion("projectVersionName").get().toString()

                    // PRE and PRO install side by side, and PRO is locked down (specs/010-entornos).
                    when (GranatumBuild.of(target)) {
                        GranatumBuild.PRE -> {
                            applicationIdSuffix = ".pre"
                            manifestPlaceholders["appLabel"] = "Granatum PRE"
                            // The LOCAL environment talks to the developer's machine over http.
                            manifestPlaceholders["usesCleartextTraffic"] = "true"
                            manifestPlaceholders["networkSecurityConfig"] = "network_security_config"
                        }
                        GranatumBuild.PRO -> {
                            manifestPlaceholders["appLabel"] = "Granatum Suite"
                            manifestPlaceholders["usesCleartextTraffic"] = "false"
                            manifestPlaceholders["networkSecurityConfig"] = "network_security_config_pro"
                        }
                    }
                }
                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = false
                    }
                }

                configureKotlinAndroid(this)
            }
        }
    }
}