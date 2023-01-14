import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jmailen.gradle.kotlinter.KotlinterExtension

plugins {
    id("org.jmailen.kotlinter") version "3.13.0"
    id("com.github.ben-manes.versions") version "0.44.0"
}

tasks.withType<DependencyUpdatesTask> {
    gradleReleaseChannel = "current"

    rejectVersionIf {
        val version = candidate.version
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        !isStable
    }
}

subprojects {
    apply(plugin = "org.jmailen.kotlinter")

    configure<KotlinterExtension> {
        experimentalRules = true
    }
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.44.2")

        classpath("com.google.gms:google-services:4.3.14")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.2")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

task<Delete>("clean") {
    delete(buildDir)
}

// Workaround until AGP 8.0
// https://issuetracker.google.com/issues/247906487
if (com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION.startsWith("7.")) {
    val loggerFactory = org.slf4j.LoggerFactory.getILoggerFactory()
    val addNoOpLogger = loggerFactory.javaClass.getDeclaredMethod("addNoOpLogger", String::class.java)
    addNoOpLogger.isAccessible = true
    addNoOpLogger.invoke(loggerFactory, "com.android.build.api.component.impl.MutableListBackedUpWithListProperty")
    addNoOpLogger.invoke(loggerFactory, "com.android.build.api.component.impl.MutableMapBackedUpWithMapProperty")
} else {
    error("AGP major version changed, remove the workaround.")
}
