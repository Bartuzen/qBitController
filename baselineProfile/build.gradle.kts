@file:Suppress("UnstableApiUsage")

import dev.bartuzen.qbitcontroller.Versions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "dev.bartuzen.baselineprofile"
    compileSdk = Versions.Android.CompileSdk

    compileOptions {
        sourceCompatibility = Versions.Android.JavaVersion
        targetCompatibility = Versions.Android.JavaVersion
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(Versions.Android.JvmTarget))
        }
    }

    defaultConfig {
        minSdk = 28
        targetSdk = Versions.Android.TargetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    targetProjectPath = ":composeApp"

    flavorDimensions += listOf("firebase")
    productFlavors {
        create("free") {
            dimension = "firebase"
        }
        create("firebase") {
            dimension = "firebase"
        }
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

androidComponents {
    onVariants { variant ->
        val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { artifactsLoader.load(it)?.applicationId!! }
        )
    }
}
