rootProject.name = "GranatumSuite"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":core:presentation")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":feature:inventory:database")
include(":feature:inventory:data")
include(":feature:inventory:domain")
include(":feature:inventory:presentation")
include(":feature:clockin:database")
include(":feature:clockin:data")
include(":feature:clockin:domain")
include(":feature:clockin:presentation")
include(":feature:auth:presentation")
include(":feature:invoicing:domain")
include(":feature:invoicing:data")
include(":feature:invoicing:presentation")
