package dev.bartuzen.qbitcontroller.plugin.ios

import dev.bartuzen.qbitcontroller.Versions
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateXcconfigTask : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        outputFile.convention(project.layout.projectDirectory.file("${project.rootDir}/iosApp/Configuration/Config.xcconfig"))
    }

    @TaskAction
    fun generateXcconfig() {
        val generated = buildString {
            appendLine("CURRENT_PROJECT_VERSION=${Versions.AppVersionCode}")
            appendLine("MARKETING_VERSION=${Versions.AppVersion}")
        }

        val xcconfigContent = javaClass.classLoader.getResourceAsStream("Config.xcconfig")!!
            .readAllBytes()
            .toString(Charsets.UTF_8)
            .replace("// generated", generated)

        val file = outputFile.get().asFile
        file.writeText(xcconfigContent)
    }
}
