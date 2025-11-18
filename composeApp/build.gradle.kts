@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalEncodingApi::class)

import android.databinding.tool.ext.joinToCamelCaseAsVar
import dev.bartuzen.qbitcontroller.Versions
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jmailen.gradle.kotlinter.tasks.ConfigurableKtLintTask
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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

    id("dev.bartuzen.qbitcontroller.language")
    id("dev.bartuzen.qbitcontroller.ios")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(Versions.Android.JvmTarget))

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
        }
    }

    jvm("desktop") {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xcontext-parameters",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=kotlin.time.ExperimentalTime"
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

                implementation(libs.coil)
                implementation(libs.coil.ktor)
                implementation(libs.coil.svg)

                implementation(libs.htmlConverter)

                implementation(project(":preferences"))

                implementation(libs.multiplatformSettings)

                implementation(libs.materialKolor)

                implementation(libs.fileKit.core)
                implementation(libs.fileKit.dialogs)

                implementation(libs.reorderable)

                implementation(libs.autolinktext)

                implementation(libs.composePipette)
            }
        }

        val nonAndroidMain by creating {
            dependsOn(commonMain)
        }

        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.okhttp)

                implementation(project.dependencies.platform(libs.okhttp.bom))
                implementation(libs.okhttp.doh)
            }
        }

        val desktopMain by getting {
            dependsOn(jvmMain)
            dependsOn(nonAndroidMain)
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
            dependsOn(nonAndroidMain)
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
    packageName("dev.bartuzen.qbitcontroller.generated")

    buildConfigField("Version", Versions.AppVersion)
    buildConfigField("SourceCodeUrl", "https://github.com/takeAChestnut/qBitController")

    // Desktop only
    buildConfigField("EnableUpdateChecker", false)
    buildConfigField("LatestReleaseUrl", "https://api.github.com/repos/takeAChestnut/qBitController/releases/latest")
}

android {
    namespace = "dev.bartuzen.qbitcontroller"
    compileSdk = Versions.Android.CompileSdk

    defaultConfig {
        applicationId = "dev.bartuzen.qbitcontroller"
        minSdk = Versions.Android.MinSdk
        targetSdk = Versions.Android.TargetSdk
        versionCode = Versions.AppVersionCode
        versionName = Versions.AppVersion
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

            val base64StoreFile = getProperty("store", "file", "base64")
            if (base64StoreFile != null) {
                val decodedBytes = Base64.decode(base64StoreFile)
                val tempFile = File.createTempFile("keystore-qbitcontroller-", ".jks")
                tempFile.outputStream().use { it.write(decodedBytes) }
                storeFile = tempFile
                tempFile.deleteOnExit()
            } else {
                storeFile = getProperty("store", "file")?.let { file(it) }
            }

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

        sourceCompatibility = Versions.Android.JavaVersion
        targetCompatibility = Versions.Android.JavaVersion
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
            val isMacOS = OperatingSystem.current().isMacOsX
            val formats = listOfNotNull(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                // https://youtrack.jetbrains.com/issue/CMP-3814
                TargetFormat.AppImage.takeIf { !isMacOS }
            ).toTypedArray()

            targetFormats(*formats)
            packageName = "qBitController"
            packageVersion = Versions.AppVersion

            linux {
                iconFile.set(project.file("icon.png"))
                modules("jdk.security.auth")
            }

            windows {
                iconFile.set(project.file("icon.ico"))
                upgradeUuid = "c4421e97-03f4-405b-9655-4db49ad3ab82"
                shortcut = true
            }

            macOS {
                iconFile.set(project.file("icon.icns"))

                val emptyResourcesElement = "<resources>\\s*</resources>|<resources/>".toRegex()
                val valuesPrefix = "values(-(b\\+)?)?".toRegex()
                val languages = project.fileTree("${project.projectDir}/src/commonMain/composeResources/")
                    .matching { include("**/strings.xml") }
                    .filterNot { it.readText().contains(emptyResourcesElement) }
                    .mapNotNull { it.parentFile.name }
                    .sorted()
                    .map {
                        it.replaceFirst(valuesPrefix, "")
                            .replace("-r", "-")
                            .replace("+", "-")
                            .takeIf(String::isNotBlank) ?: "en"
                    }

                infoPlist {
                    this.extraKeysRawXml = buildString {
                        appendLine("<key>CFBundleLocalizations</key>")
                        appendLine("<array>")
                        languages.forEach {
                            appendLine("    <string>$it</string>")
                        }
                        appendLine("</array>")
                    }
                }
            }
        }

        buildTypes {
            release {
                proguard {
                    configurationFiles.from("proguard-rules-desktop.pro")
                }
            }
        }
    }
}

afterEvaluate {
    tasks.withType<ConfigurableKtLintTask> {
        source = source.minus(fileTree("build")).asFileTree
    }
}

listOf("" to "main", "Release" to "main-release").forEach { (buildType, buildFolder) ->
    val appId = "dev.bartuzen.qbitcontroller"
    val flatpakDir = "$buildDir/flatpak"

    tasks.register("prepare${buildType}Flatpak") {
        dependsOn("package${buildType}AppImage")
        doLast {
            delete {
                delete("$flatpakDir/bin/")
                delete("$flatpakDir/lib/")
            }
            copy {
                from("$buildDir/compose/binaries/$buildFolder/app/qBitController/")
                into("$flatpakDir/")
            }
            copy {
                from("$projectDir/src/desktopMain/composeResources/drawable/icon-rounded.svg")
                into("$flatpakDir/")
                rename { "icon.svg" }
            }
            copy {
                from("$projectDir/src/desktopMain/resources/flatpak/manifest.yml")
                into("$flatpakDir/")
                rename { "$appId.yml" }
            }
            copy {
                from("$projectDir/src/desktopMain/resources/flatpak/qbitcontroller.desktop")
                into("$flatpakDir/")
                rename { "$appId.desktop" }
            }
        }
    }

    tasks.register("bundle${buildType}Flatpak") {
        dependsOn("prepare${buildType}Flatpak")
        doLast {
            exec {
                workingDir(flatpakDir)
                val buildCommand = listOf(
                    "flatpak-builder",
                    "--disable-rofiles-fuse",
                    "--force-clean",
                    "--state-dir=build/flatpak-builder",
                    "--repo=build/flatpak-repo",
                    "build/flatpak-target",
                    "$appId.yml",
                )
                commandLine(buildCommand)
            }
            exec {
                workingDir(flatpakDir)
                val bundleCommand = listOf(
                    "flatpak",
                    "build-bundle",
                    "build/flatpak-repo",
                    "qBitController.flatpak",
                    appId,
                )
                commandLine(bundleCommand)
            }
        }
    }

    tasks.register("install${buildType}Flatpak") {
        dependsOn("prepare${buildType}Flatpak")
        doLast {
            exec {
                workingDir(flatpakDir)
                val installCommand = listOf(
                    "flatpak-builder",
                    "--install",
                    "--user",
                    "--force-clean",
                    "--state-dir=build/flatpak-builder",
                    "--repo=build/flatpak-repo",
                    "build/flatpak-target",
                    "$appId.yml",
                )
                commandLine(installCommand)
            }
        }
    }

    tasks.register("run${buildType}Flatpak") {
        dependsOn("install${buildType}Flatpak")
        doLast {
            exec {
                val runCommand = listOf(
                    "flatpak",
                    "run",
                    appId,
                )
                commandLine(runCommand)
            }
        }
    }
}

tasks.matching { it.name.contains("Flatpak") }.configureEach {
    notCompatibleWithConfigurationCache("")
}
