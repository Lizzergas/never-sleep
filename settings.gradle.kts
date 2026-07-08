rootProject.name = "NeverSleep"
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
        maven("https://repo.gradle.org/gradle/libs-releases")
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
        maven("https://repo.gradle.org/gradle/libs-releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":app:androidApp")
include(":app:desktopApp")
include(":app:shared")
include(":core:common")
include(":core:connectivity")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:model")
include(":core:navigation")
include(":core:network")
include(":core:ui")
include(":feature:auth")
include(":feature:notes")
include(":feature:onboarding")
include(":feature:settings")
include(":feature:showcase")
include(":server")
