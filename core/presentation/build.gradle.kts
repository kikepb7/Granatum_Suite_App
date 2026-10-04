import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    // `mobileMain` carries the `actual`s shared by Android and iOS (the moko-backed
    // PermissionController). Declaring it as a group on the default hierarchy template
    // — rather than hand-wiring dependsOn edges — lets Kotlin keep owning androidMain
    // and iosMain; explicit dependsOn calls make it bail out of the template entirely.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("mobile") {
                withAndroidTarget()
                withIos()
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.core.domain)

                implementation(libs.material3.adaptive)
                implementation(libs.bundles.koin.common)

                implementation(compose.components.resources)

                implementation(libs.moko.permissions)
                implementation(libs.moko.permissions.compose)
                implementation(libs.moko.permissions.notifications)
            }
        }
    }
}
