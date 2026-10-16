plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.core.domain)

                implementation(libs.bundles.ktor.common)
                // The demo build's in-memory backend runs on Ktor's mock engine (DemoMode).
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.datetime)
                implementation(libs.touchlab.kermit)
                implementation(libs.koin.core)

                implementation(libs.datastore.preferences)

                implementation(libs.androidx.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.koin.android)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
