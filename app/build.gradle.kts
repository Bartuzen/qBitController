@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kapt)
    alias(libs.plugins.kotlinter)
}

android {
    namespace = "dev.bartuzen.qbitcontroller"
    compileSdk = 33

    defaultConfig {
        applicationId = "dev.bartuzen.qbitcontroller"
        minSdk = 21
        targetSdk = 33
        versionCode = 13
        versionName = "0.8.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = false
                isOptimizeCode = true
                proguardFile("proguard-rules.pro")
            }

            signingConfig = signingConfigs.create("release")
        }
    }

    flavorDimensions += "firebase"
    productFlavors {
        create("free") {
            dimension = "firebase"
        }
        create("firebase") {
            dimension = "firebase"
        }
    }

    signingConfigs {
        getByName("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()

            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
            }

            fun getProperty(propertyName: String, envName: String) =
                keystoreProperties.getProperty(propertyName) ?: System.getenv("QBITCONTROLLER_$envName")

            storeFile = getProperty("storeFile", "STORE_FILE")?.let { file(it) }
            storePassword = getProperty("storePassword", "STORE_PASSWORD")
            keyAlias = getProperty("keyAlias", "KEY_ALIAS")
            keyPassword = getProperty("keyPassword", "KEY_PASSWORD")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        disable += "MissingTranslation"
    }
}

kapt {
    correctErrorTypes = true
}

val isFirebaseEnabled = gradle.startParameter.taskRequests.any { task ->
    task.args.any { arg ->
        arg.contains("Firebase")
    }
}
if (isFirebaseEnabled) {
    apply(plugin = libs.plugins.firebase.googleServices.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerView)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.swipeRefreshLayout)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.lifecycle.viewModel)

    implementation(libs.material)

    debugImplementation(libs.leakCanary)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.work.runtime)
    implementation(libs.work.hilt.core)
    kapt(libs.work.hilt.compiler)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.retrofit.converter.jackson)

    implementation(libs.jackson.kotlin)

    implementation(libs.viewBindingPropertyDelegate.noReflection)

    coreLibraryDesugaring(libs.desugar)

    val firebaseImplementation by configurations
    firebaseImplementation(platform(libs.firebase.bom))
    firebaseImplementation(libs.firebase.analytics)
    firebaseImplementation(libs.firebase.crashlytics)
}
