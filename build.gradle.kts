import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.gradleVersions)

    alias(libs.plugins.firebase.googleServices) apply false
    alias(libs.plugins.firebase.crashlytics) apply false

    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
}

tasks.withType<DependencyUpdatesTask> {
    gradleReleaseChannel = "current"

    rejectVersionIf {
        fun String.isStableVersion(): Boolean {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { contains(it, ignoreCase = true) }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            return stableKeyword || regex.matches(this)
        }

        candidate.version.isStableVersion() != currentVersion.isStableVersion()
    }
}

task<Delete>("clean") {
    delete(layout.buildDirectory)
}
