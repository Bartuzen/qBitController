@file:Suppress("UnstableApiUsage")

import android.databinding.tool.ext.joinToCamelCaseAsVar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jmailen.gradle.kotlinter.tasks.ConfigurableKtLintTask
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)

    id("dev.bartuzen.qbitcontroller.language")
    id("dev.bartuzen.qbitcontroller.ios")
}

val appVersion = "1.1.1"
val appVersionCode = 21

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)

            freeCompilerArgs.addAll(
                "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
            )
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true

            binaryOption("bundleShortVersionString", appVersion)
            binaryOption("bundleVersion", appVersionCode.toString())
        }
    }

    jvm("desktop")

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(layout.buildDirectory.file("generated/kotlin"))

            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewModel)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(compose.materialIconsExtended)

                implementation(libs.compose.ui.util)
                implementation(libs.compose.ui.backHandler)
                implementation(libs.compose.windowSizeClass)

                implementation(libs.compose.navigation)

                implementation(libs.coroutines.core)

                implementation(libs.kotlinxSerialization)
                implementation(libs.kotlinx.datetime)

                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewModel)
                implementation(libs.koin.compose.viewModel.navigation)

                implementation(libs.ktor.core)
                implementation(libs.ktor.contentNegotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.ktor.auth)

                implementation(libs.ktorfit)
                implementation(libs.ktorfit.converter.response)

                implementation(libs.coil)
                implementation(libs.coil.ktor)
                implementation(libs.coil.svg)

                implementation(libs.htmlConverter)

                implementation(libs.composePreferences)

                implementation(libs.multiplatformSettings)

                implementation(libs.materialKolor)

                implementation(libs.fileKit.core)
                implementation(libs.fileKit.dialogs)

                implementation(libs.reorderable)
            }
        }

        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.okhttp)

                implementation(libs.okhttp.doh)
            }
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        val androidMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.appcompat)
                implementation(libs.coroutines.android)

                implementation(libs.koin.android)
                implementation(libs.koin.androidx.workManager)

                implementation(libs.material)

                implementation(libs.androidx.profileinstaller)

                implementation(libs.accompanist.permissions)

                implementation(libs.work.runtime)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.darwin)
            }
        }

        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

buildConfig {
    buildConfigField("Version", appVersion)
    buildConfigField("SourceCodeUrl", "https://github.com/Bartuzen/qBitController")
}

android {
    namespace = "dev.bartuzen.qbitcontroller"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.bartuzen.qbitcontroller"
        minSdk = 21
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion
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
        create("release") {
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
            }

            if (System.getenv("QBITCONTROLLER_SIGN_RELEASE") == "true") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    lint {
        disable += listOf("MissingTranslation", "ExtraTranslation")
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
    debugImplementation(compose.uiTooling)
    baselineProfile(project(":baselineProfile"))

    val firebaseImplementation by configurations
    firebaseImplementation(platform(libs.firebase.bom))
    firebaseImplementation(libs.firebase.analytics)
    firebaseImplementation(libs.firebase.crashlytics)
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

compose.desktop {
    application {
        mainClass = "dev.bartuzen.qbitcontroller.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "qBitController"
            packageVersion = appVersion

            linux {
                modules("jdk.security.auth")
            }

            windows {
                shortcut = true
            }

            macOS {
                iconFile.set(project.file("icon.icns"))
            }
        }
    }
}

tasks.withType<ConfigurableKtLintTask> {
    source = source.minus(fileTree("build")).asFileTree
}

afterEvaluate {
    tasks.matching { task ->
        task.name.startsWith("ksp") && task.name.contains("KotlinMetadata")
    }.configureEach {
        dependsOn(":composeApp:generateLanguageList")
    }
}
