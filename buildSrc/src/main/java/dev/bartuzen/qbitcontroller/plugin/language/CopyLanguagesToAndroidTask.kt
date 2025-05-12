package dev.bartuzen.qbitcontroller.plugin.language

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@CacheableTask
abstract class CopyLanguagesToAndroidTask : DefaultTask() {

    @get:OutputDirectory
    val androidResourcesDir = project.file("${project.projectDir}/src/androidMain/res")

    @get:Input
    val keysToCopy = listOf("app_name", "share_add_torrent")

    @get:Input
    val languageEntries = project.fileTree(project.file("${project.projectDir}/src/commonMain/composeResources"))
        .matching { include("**/strings.xml") }
        .map { sourceFile ->
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()

            val extractedStrings = mutableMapOf<String, String>()

            val valuesDirectoryName = sourceFile.parentFile.name
            if (valuesDirectoryName.matches(Regex("values.*"))) {
                val doc = dBuilder.parse(sourceFile)
                doc.documentElement.normalize()

                val stringElements = doc.getElementsByTagName("string")

                for (i in 0 until stringElements.length) {
                    val element = stringElements.item(i) as Element
                    val key = element.getAttribute("name")

                    if (keysToCopy.contains(key)) {
                        extractedStrings[key] = element.textContent
                    }
                }
            }

            valuesDirectoryName to extractedStrings
        }

    @TaskAction
    fun copyLanguageToAndroid() {
        deleteGeneratedStringFiles(androidResourcesDir)

        languageEntries.forEach { (valuesDirectoryName, extractedStrings) ->
            if (extractedStrings.isNotEmpty()) {
                val targetDir = File(androidResourcesDir, valuesDirectoryName)
                targetDir.mkdirs()
                val targetFile = File(targetDir, "strings_generated.xml")

                val xmlContent = buildString {
                    appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                    appendLine("<resources>")
                    extractedStrings.forEach { (key, value) ->
                        appendLine("""    <string name="$key">$value</string>""")
                    }
                    appendLine("</resources>")
                }

                targetFile.writeText(xmlContent)
            }
        }
    }

    private fun deleteGeneratedStringFiles(directory: File) {
        if (!directory.exists() || !directory.isDirectory) {
            return
        }

        val files = directory.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                deleteGeneratedStringFiles(file)

                if (file.exists() && file.list()?.isEmpty() == true) {
                    file.delete()
                }
            } else if (file.name == "strings_generated.xml") {
                file.delete()
            }
        }
    }
}
