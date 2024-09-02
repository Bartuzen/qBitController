@file:Suppress("UnstableApiUsage")

import android.databinding.tool.ext.joinToCamelCaseAsVar
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.baselineprofile)

    id("dev.bartuzen.qbitcontroller.localesconfig")
}

android {
    namespace = "dev.bartuzen.qbitcontroller"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.bartuzen.qbitcontroller"
        minSdk = 21
        targetSdk = 35
        versionCode = 18
        versionName = "0.8.5"

        buildConfigField("String", "SOURCE_CODE_URL", "\"https://github.com/Bartuzen/qBitController\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDefault = true
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
            isDefault = true
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

            fun getProperty(vararg name: String): String? {
                val propertyName = name.toList().joinToCamelCaseAsVar()
                val envName = "QBITCONTROLLER_" + name.joinToString("_").uppercase(Locale.US)
                return keystoreProperties.getProperty(propertyName) ?: System.getenv(envName)
            }

            storeFile = getProperty("store", "file")?.let { file(it) }
            storePassword = getProperty("store", "password")
            keyAlias = getProperty("key", "alias")
            keyPassword = getProperty("key", "password")
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
        buildConfig = true
        compose = true
    }

    lint {
        disable += listOf("MissingTranslation", "ExtraTranslation")
    }
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

    api(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.toolingPreview)

    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineProfile"))

    implementation(libs.compose.materialIcons.core)
    implementation(libs.compose.materialIcons.extended)
    
    implementation(libs.compose.navigation)

    implementation(libs.compose.hilt)

    implementation(libs.accompanist.permissions)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.kotlinxSerialization)

    implementation(libs.work.runtime)
    implementation(libs.work.hilt.core)
    ksp(libs.work.hilt.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.retrofit.converter.kotlinxSerialization)

    implementation(libs.okhttp.doh)

    implementation(libs.viewBindingPropertyDelegate.noReflection)

    coreLibraryDesugaring(libs.desugar)

    implementation(libs.coil)
    implementation(libs.coil.svg)

    val firebaseImplementation by configurations
    firebaseImplementation(platform(libs.firebase.bom))
    firebaseImplementation(libs.firebase.analytics)
    firebaseImplementation(libs.firebase.crashlytics)
}
