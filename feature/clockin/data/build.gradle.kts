plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)

                implementation(projects.core.data)
                implementation(projects.core.domain)
                implementation(projects.feature.clockin.domain)
                implementation(projects.feature.clockin.database)

                implementation(libs.androidx.room.runtime)
                implementation(libs.sqlite.bundled)

                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.koin.android)
                implementation(libs.androidx.work.runtime.ktx)
            }
        }

        iosMain {
            dependencies {}
        }
    }
}
