
import com.granatum.buildlogic.convention.configureAndroidTarget
import com.granatum.buildlogic.convention.configureIosTargets
import com.granatum.buildlogic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class CmpApplicationConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.granatum.buildlogic.convention.android.application.compose")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.kotlin.plugin.serialization")
                apply("com.granatum.buildlogic.convention.quality")
            }

            configureAndroidTarget()
            configureIosTargets()

            dependencies {
                "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
                // KmpLibraryConventionPlugin already gives every library module kotlin.test;
                // the app module was missing it, so composeApp's commonTest never compiled.
                "commonTestImplementation"(libs.findLibrary("kotlin-test").get())
            }
        }
    }
}