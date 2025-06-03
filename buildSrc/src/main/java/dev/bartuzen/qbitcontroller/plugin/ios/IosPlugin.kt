package dev.bartuzen.qbitcontroller.plugin.ios

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class IosPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<GenerateInfoPlistTask>("generateInfoPlist") {
            group = "other"
            description = "Generates Info.plist file"
        }
    }
}
