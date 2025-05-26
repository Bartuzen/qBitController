@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://gitlab.com/api/v4/projects/69663547/packages/maven")
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap") {
            mavenContent {
                includeGroupAndSubgroups("io.ktor")
            }
        }
    }
}

rootProject.name = "qBitController"
include(":composeApp")
include(":baselineProfile")
