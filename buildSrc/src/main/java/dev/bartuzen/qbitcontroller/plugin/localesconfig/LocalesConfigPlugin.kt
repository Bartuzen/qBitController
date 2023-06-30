package dev.bartuzen.qbitcontroller.plugin.localesconfig

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class LocalesConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val localesConfigTask = project.tasks.register<GenerateLocalesConfigTask>("generateLocalesConfig") {
            group = "other"
            description = "Generates the locales configuration file"
        }

        project.tasks.getByName("preBuild").dependsOn(localesConfigTask)
    }
}

