import dev.bartuzen.qbitcontroller.Versions

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget()
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(libs.androidx.lifecycle.viewModel)
            }
        }
    }
}

android {
    namespace = "dev.bartuzen.qbitcontroller.preferences"
    compileSdk = Versions.Android.CompileSdk

    defaultConfig {
        minSdk = Versions.Android.MinSdk
    }

    compileOptions {
        sourceCompatibility = Versions.Android.JavaVersion
        targetCompatibility = Versions.Android.JavaVersion
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

