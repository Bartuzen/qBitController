plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "dev.bartuzen.qbitcontroller"
    compileSdk = 33

    defaultConfig {
        applicationId = "dev.bartuzen.qbitcontroller"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = false
                isOptimizeCode = true
                proguardFile("proguard-rules.pro")
            }
        }

        create("debugMinified") {
            initWith(getByName("debug"))
            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = false
                isOptimizeCode = true
                proguardFile("proguard-rules.pro")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.2.1")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation("androidx.fragment:fragment-ktx:1.5.4")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")

    implementation("com.google.dagger:hilt-android:2.44.1")
    kapt("com.google.dagger:hilt-compiler:2.44.1")

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("androidx.preference:preference-ktx:1.2.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.hannesdorfmann.fragmentargs:annotation:4.0.0-RC1")
    kapt("com.hannesdorfmann.fragmentargs:processor:4.0.0-RC1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.9.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
}
