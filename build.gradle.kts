import org.jmailen.gradle.kotlinter.KotlinterExtension

plugins {
    id("org.jmailen.kotlinter") version "3.12.0"
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
        classpath("com.android.tools.build:gradle:7.3.1")
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
