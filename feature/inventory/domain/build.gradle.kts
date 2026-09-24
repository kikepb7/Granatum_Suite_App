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

                implementation(projects.core.domain)

                implementation(libs.koin.core)
            }
        }

        androidMain {
            dependencies {}
        }

        iosMain {
            dependencies {}
        }
    }
}
