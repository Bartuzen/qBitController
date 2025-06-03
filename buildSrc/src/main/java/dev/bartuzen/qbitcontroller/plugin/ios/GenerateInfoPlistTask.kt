package dev.bartuzen.qbitcontroller.plugin.ios

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateInfoPlistTask : DefaultTask() {

    private val emptyResourcesElement = "<resources>\\s*</resources>|<resources/>".toRegex()
    private val valuesPrefix = "values(-(b\\+)?)?".toRegex()

    @get:Input
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

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        outputFile.convention(project.layout.projectDirectory.file("${project.rootDir}/iosApp/iosApp/Info.plist"))
    }

    @TaskAction
    fun generateInfoPlist() {
        val localizations = buildString {
            appendLine("<key>CFBundleLocalizations</key>")
            appendLine("<array>")
            languages.forEach {
                appendLine("    <string>$it</string>")
            }
            appendLine("</array>")
        }.prependIndent("    ")

        val infoPlistContent = javaClass.classLoader.getResourceAsStream("Info.plist")!!
            .readAllBytes()
            .toString(Charsets.UTF_8)
            .replace("<!-- {{LOCALIZATIONS}} -->", localizations)

        val file = outputFile.get().asFile
        file.writeText(infoPlistContent)
    }
}
