@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
    kotlin("kapt")
}

android {
    namespace = "dev.bartuzen.qbitcontroller"
    compileSdk = 33

    defaultConfig {
        applicationId = "dev.bartuzen.qbitcontroller"
        minSdk = 21
        targetSdk = 33
        versionCode = 11
        versionName = "0.7.2"
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

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
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
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation("androidx.fragment:fragment-ktx:1.5.6")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.hilt:hilt-work:1.0.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")

    implementation("com.google.dagger:hilt-android:2.45")
    kapt("com.google.dagger:hilt-compiler:2.45")

    implementation("androidx.preference:preference-ktx:1.2.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.9.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")

    implementation("com.github.kirich1409:viewbindingpropertydelegate-noreflection:1.5.8")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.2")

    val firebaseImplementation by configurations
    firebaseImplementation(platform("com.google.firebase:firebase-bom:31.2.3"))
    firebaseImplementation("com.google.firebase:firebase-crashlytics-ktx")
    firebaseImplementation("com.google.firebase:firebase-analytics-ktx")
}
