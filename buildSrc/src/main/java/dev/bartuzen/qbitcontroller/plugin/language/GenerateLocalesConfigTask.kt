package dev.bartuzen.qbitcontroller.plugin.language

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class GenerateLocalesConfigTask : DefaultTask() {

    private val emptyResourcesElement = "<resources>\\s*</resources>|<resources/>".toRegex()
    private val valuesPrefix = "values(-(b\\+)?)?".toRegex()

    @get:Input
    val languages = project.fileTree("${project.projectDir}/src/commonMain/composeResources")
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
    val outputFile: File = project.file("${project.projectDir}/src/androidMain/res/xml/locales_config.xml")

    @TaskAction
    fun generateLocalesConfig() {
        val content = buildString {
            append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            append("<locale-config xmlns:android=\"http://schemas.android.com/apk/res/android\">\n")
            languages.forEach { language ->
                append("    <locale android:name=\"$language\" />\n")
            }
            append("</locale-config>\n")
        }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(content)
    }
}
