plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "dev.bartuzen.qbitcontroller"
        minSdk = 21
        targetSdk = 31
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.1.0-beta04"
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

    implementation("androidx.fragment:fragment-ktx:1.4.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.0")

    implementation("com.google.dagger:hilt-android:2.40.2")
    kapt("com.google.dagger:hilt-compiler:2.40.2")

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.hannesdorfmann.fragmentargs:annotation:4.0.0-RC1")
    kapt("com.hannesdorfmann.fragmentargs:processor:4.0.0-RC1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.9.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")

    implementation("androidx.compose.ui:ui:1.1.0-beta04")
    implementation("androidx.compose.ui:ui-tooling:1.1.0-beta04")
    implementation("androidx.compose.ui:ui-tooling-preview:1.1.0-beta04")
    implementation("androidx.compose.foundation:foundation:1.1.0-beta04")
    implementation("androidx.compose.material:material:1.1.0-beta04")
    implementation("androidx.compose.material:material-icons-core:1.1.0-beta04")
    implementation("androidx.compose.material:material-icons-extended:1.1.0-beta04")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.4.0")
    implementation("androidx.navigation:navigation-compose:2.4.0-beta02")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0-beta01")
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.0-rc02")

    implementation("com.google.accompanist:accompanist-swiperefresh:0.21.2-beta")
    implementation("com.google.accompanist:accompanist-pager:0.21.3-beta")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.21.3-beta")

    implementation("me.onebone:toolbar-compose:2.2.0")
}