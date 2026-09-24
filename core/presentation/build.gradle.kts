plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
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

        val mobileMain by creating {
            dependencies {
                implementation(libs.moko.permissions)
                implementation(libs.moko.permissions.compose)
                implementation(libs.moko.permissions.notifications)
            }
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(mobileMain)

        val iosMain by creating {
            dependsOn(mobileMain)
        }

        listOf(
            iosArm64(),
            iosX64(),
            iosSimulatorArm64()
        ).forEach { target ->
            getByName("${target.name}Main") {
                dependsOn(iosMain)
            }
        }
    }
}