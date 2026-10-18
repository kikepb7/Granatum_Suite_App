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
include(":core:demo")
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
include(":feature:staff:domain")
include(":feature:staff:data")
include(":feature:staff:presentation")
include(":feature:sheets:domain")
include(":feature:sheets:data")
include(":feature:sheets:presentation")
include(":feature:events:domain")
include(":feature:events:database")
include(":feature:events:data")
include(":feature:events:presentation")
