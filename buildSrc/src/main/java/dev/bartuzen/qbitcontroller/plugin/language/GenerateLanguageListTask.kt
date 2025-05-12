package dev.bartuzen.qbitcontroller.plugin.language

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Locale

@CacheableTask
abstract class GenerateLanguageListTask : DefaultTask() {

    private val emptyResourcesElement = "<resources>\\s*</resources>|<resources/>".toRegex()
    private val valuesPrefix = "values(-(b\\+)?)?".toRegex()

    @get:Input
    val languages = project.fileTree("${project.projectDir}/src/commonMain/composeResources/")
        .matching { include("**/strings.xml") }
        .filterNot { it.readText().contains(emptyResourcesElement) }
        .mapNotNull { it.parentFile.name }
        .map {
            it.replaceFirst(valuesPrefix, "")
                .replace("-r", "-")
                .replace("+", "-")
                .takeIf(String::isNotBlank) ?: "en"
        }
        .sorted()

    @get:OutputFile
    val languagesKtFile: File =
        project.layout.buildDirectory.file("generated/kotlin/dev/bartuzen/qbitcontroller/generated/Languages.kt")
            .get().asFile

    @TaskAction
    fun generateLanguageListTask() {
        val content = buildString {
            appendLine("package dev.bartuzen.qbitcontroller.generated")
            appendLine()
            appendLine("val SupportedLanguages = mapOf(")
            languages.forEachIndexed { _, lang ->
                appendLine("""    "$lang" to "${getLanguageDisplayName(lang)}",""")
            }
            appendLine(")")
        }

        languagesKtFile.parentFile.mkdirs()
        languagesKtFile.writeText(content)
    }

    private fun getLanguageDisplayName(language: String): String? {
        val locale = when (language) {
            "zh-CN" -> Locale.forLanguageTag("zh-Hans")
            "zh-TW" -> Locale.forLanguageTag("zh-Hant")
            else -> Locale.forLanguageTag(language)
        }
        return locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
    }
}
