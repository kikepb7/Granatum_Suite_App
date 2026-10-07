import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.granatum.buildlogic.convention.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.androidx.room.gradle.plugin)
    implementation(libs.buildkonfig.gradlePlugin)
    implementation(libs.buildkonfig.compiler)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "com.granatum.buildlogic.convention.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidComposeApplication") {
            id = "com.granatum.buildlogic.convention.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("cmpApplication") {
            id = "com.granatum.buildlogic.convention.cmp.application"
            implementationClass = "CmpApplicationConventionPlugin"
        }
        register("kmpLibrary") {
            id = "com.granatum.buildlogic.convention.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }

        register("cmpLibrary") {
            id = "com.granatum.buildlogic.convention.cmp.library"
            implementationClass = "CmpLibraryConventionPlugin"
        }

        register("cmpFeature") {
            id = "com.granatum.buildlogic.convention.cmp.feature"
            implementationClass = "CmpFeatureConventionPlugin"
        }

        register("buildKonfig") {
            id = "com.granatum.buildlogic.convention.buildkonfig"
            implementationClass = "BuildKonfigConventionPlugin"
        }

        register("room") {
            id = "com.granatum.buildlogic.convention.room"
            implementationClass = "RoomConventionPlugin"
        }

        register("quality") {
            id = "com.granatum.buildlogic.convention.quality"
            implementationClass = "QualityConventionPlugin"
        }
    }
}