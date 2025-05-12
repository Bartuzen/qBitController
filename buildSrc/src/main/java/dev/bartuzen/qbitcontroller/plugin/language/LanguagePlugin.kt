package dev.bartuzen.qbitcontroller.plugin.language

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class LanguagePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val localesConfigTask = project.tasks.register<GenerateLocalesConfigTask>("generateLocalesConfig") {
            group = "other"
            description = "Generates the locales configuration file"
        }

        val languageListTask = project.tasks.register<GenerateLanguageListTask>("generateLanguageList") {
            group = "other"
            description = "Generates the language list file"
        }

        val copyLanguagesToAndroidTask = project.tasks.register<CopyLanguagesToAndroidTask>("copyLanguagesToAndroid") {
            group = "other"
            description = "Copies the strings files to the Android project"
        }

        project.tasks.getByName("preBuild").dependsOn(localesConfigTask, languageListTask, copyLanguagesToAndroidTask)
    }
}

