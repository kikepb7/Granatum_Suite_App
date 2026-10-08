plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.datetime)

                implementation(projects.core.data)
                implementation(projects.core.domain)
                implementation(projects.feature.inventory.domain)
                implementation(projects.feature.inventory.database)

                implementation(libs.androidx.room.runtime)
                implementation(libs.sqlite.bundled)

                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
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
                implementation(libs.koin.android)
            }
        }

        iosMain {
            dependencies {}
        }
    }
}
