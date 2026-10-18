import com.granatum.buildlogic.convention.GranatumBuild

plugins {
    alias(libs.plugins.convention.cmp.application)
    alias(libs.plugins.compose.hot.reload)
}

// PRE (internal, with the environment selector and the demo) or PRO (store) — specs/010-entornos.
val granatumBuild = GranatumBuild.of(project)

kotlin {
    sourceSets {
        // One Koin module per build: PRE's registers the demo module, PRO's is empty, so the store
        // app neither links nor references `:core:demo`.
        commonMain {
            kotlin.srcDir("src/environment/${granatumBuild.id}/kotlin")
        }
        if (granatumBuild == GranatumBuild.PRE) {
            commonMain.dependencies { implementation(projects.core.demo) }
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.core.splashscreen)

            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            // CORE
            implementation(projects.core.data)
            implementation(projects.core.domain)
            implementation(projects.core.designsystem)
            implementation(projects.core.presentation)

            implementation(libs.jetbrains.compose.navigation)
            implementation(libs.jetbrains.compose.material.icons.core)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.bundles.koin.common)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // AUTH feature
            implementation(projects.feature.auth.presentation)

            // INVENTORY feature
            implementation(projects.feature.inventory.data)
            implementation(projects.feature.inventory.domain)
            implementation(projects.feature.inventory.presentation)

            // CLOCK-IN (fichaje) feature
            implementation(projects.feature.clockin.data)
            implementation(projects.feature.clockin.domain)
            implementation(projects.feature.clockin.presentation)

            implementation(projects.feature.invoicing.data)
            implementation(projects.feature.invoicing.domain)
            implementation(projects.feature.invoicing.presentation)

            implementation(projects.feature.staff.data)
            implementation(projects.feature.staff.domain)
            implementation(projects.feature.staff.presentation)

            implementation(projects.feature.events.data)
            implementation(projects.feature.events.domain)
            implementation(projects.feature.events.presentation)

            implementation(projects.feature.sheets.data)
            implementation(projects.feature.sheets.domain)
            implementation(projects.feature.sheets.presentation)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.jetbrains.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.compose)

            implementation(libs.koin.core)
        }
    }
}
