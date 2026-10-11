plugins {
    alias(libs.plugins.convention.kmp.library)
}

// The demo users and their in-memory backend (specs/010-entornos, research D5). Only the PRE build
// of composeApp depends on this module, so the store app never contains it.
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)

                implementation(projects.core.domain)
                implementation(projects.core.data)

                implementation(libs.bundles.ktor.common)
                implementation(libs.ktor.client.mock)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
