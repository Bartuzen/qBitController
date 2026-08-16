package dev.bartuzen.qbitcontroller

import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class WindowsDefaultTorrentHandlersRegistrationResult {
    Registered,
    OpenedDefaultAppsSettings,
    Unavailable,
}

fun registerWindowsDefaultTorrentHandlers(): WindowsDefaultTorrentHandlersRegistrationResult {
    val executable = getPackagedExecutable() ?: run {
        return if (openWindowsDefaultAppsSettings()) {
            WindowsDefaultTorrentHandlersRegistrationResult.OpenedDefaultAppsSettings
        } else {
            WindowsDefaultTorrentHandlersRegistrationResult.Unavailable
        }
    }

    registerWindowsMagnetProtocolHandler(executable)
    registerWindowsTorrentFileHandler(executable)
    registerWindowsAppCapabilities(executable)
    if (!isWindowsMachineDefaultAppRegistered()) {
        registerWindowsMachineAppCapabilities(executable)
    }

    return if (areWindowsDefaultTorrentHandlersRegistered(executable)) {
        WindowsDefaultTorrentHandlersRegistrationResult.Registered
    } else if (openWindowsDefaultAppsSettingsForRegisteredApp()) {
        WindowsDefaultTorrentHandlersRegistrationResult.OpenedDefaultAppsSettings
    } else {
        WindowsDefaultTorrentHandlersRegistrationResult.Unavailable
    }
}

fun areWindowsDefaultTorrentHandlersRegistered(): Boolean {
    val executable = getPackagedExecutable() ?: return false

    return areWindowsDefaultTorrentHandlersRegistered(executable)
}

private fun areWindowsDefaultTorrentHandlersRegistered(executable: File): Boolean {
    return isWindowsMagnetProtocolHandlerRegistered(executable) &&
        isWindowsTorrentFileHandlerRegistered(executable)
}

private fun registerWindowsMagnetProtocolHandler(executable: File): Boolean {
    val commandValue = openCommandValue(executable)
    val progId = "qBitController.magnet"

    return addRegistryValue(
        key = "HKCU\\Software\\Classes\\magnet",
        name = null,
        value = "URL:Magnet Protocol",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\magnet",
        name = "URL Protocol",
        value = "",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\magnet\\DefaultIcon",
        name = null,
        value = "${executable.absolutePath},0",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\magnet\\shell\\open\\command",
        name = null,
        value = commandValue,
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\magnet\\OpenWithProgids",
        name = progId,
        value = "",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\$progId",
        name = null,
        value = "URL:Magnet Protocol",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\$progId",
        name = "URL Protocol",
        value = "",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\$progId\\DefaultIcon",
        name = null,
        value = "${executable.absolutePath},0",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\$progId\\shell\\open\\command",
        name = null,
        value = commandValue,
    )
}

private fun registerWindowsTorrentFileHandler(executable: File): Boolean {
    val commandValue = openCommandValue(executable)
    val progId = "qBitController.torrent"

    return addRegistryValue(
        key = "HKCU\\Software\\Classes\\.torrent",
        name = null,
        value = progId,
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\.torrent",
        name = "Content Type",
        value = "application/x-bittorrent",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\.torrent\\OpenWithProgids",
        name = progId,
        value = "",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\$progId",
        name = null,
        value = "BitTorrent File",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\$progId\\DefaultIcon",
        name = null,
        value = "${executable.absolutePath},0",
    ) && addRegistryValue(
        key = "HKCU\\Software\\Classes\\$progId\\shell\\open\\command",
        name = null,
        value = commandValue,
    )
}

private fun registerWindowsAppCapabilities(executable: File): Boolean {
    val capabilitiesKey = "HKCU\\Software\\qBitController\\Capabilities"
    val appPathsKey = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\qBitController.exe"
    val appPathsCapabilitiesKey = "$appPathsKey\\Capabilities"
    val applicationsKey = "HKCU\\Software\\Classes\\Applications\\qBitController.exe"
    val applicationsCapabilitiesKey = "$applicationsKey\\Capabilities"

    return addRegistryValue(
        key = capabilitiesKey,
        name = "ApplicationName",
        value = "qBitController",
    ) && addRegistryValue(
        key = capabilitiesKey,
        name = "ApplicationDescription",
        value = "Open .torrent files and magnet links with qBitController.",
    ) && addRegistryValue(
        key = capabilitiesKey,
        name = "ApplicationIcon",
        value = "${executable.absolutePath},0",
    ) && addRegistryValue(
        key = "$capabilitiesKey\\FileAssociations",
        name = ".torrent",
        value = "qBitController.torrent",
    ) && addRegistryValue(
        key = "$capabilitiesKey\\URLAssociations",
        name = "magnet",
        value = "qBitController.magnet",
    ) && addRegistryValue(
        key = "HKCU\\Software\\RegisteredApplications",
        name = "qBitController",
        value = "Software\\qBitController\\Capabilities",
    ) && addRegistryValue(
        key = appPathsKey,
        name = null,
        value = executable.absolutePath,
    ) && addRegistryValue(
        key = appPathsKey,
        name = "Path",
        value = executable.parentFile.absolutePath,
    ) && addRegistryValue(
        key = appPathsKey,
        name = "Capabilities",
        value = "Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\qBitController.exe\\Capabilities",
    ) && addRegistryValue(
        key = appPathsCapabilitiesKey,
        name = "ApplicationName",
        value = "qBitController",
    ) && addRegistryValue(
        key = appPathsCapabilitiesKey,
        name = "ApplicationDescription",
        value = "qBitController",
    ) && addRegistryValue(
        key = appPathsCapabilitiesKey,
        name = "ApplicationIcon",
        value = "${executable.absolutePath},0",
    ) && addRegistryValue(
        key = "$appPathsCapabilitiesKey\\FileAssociations",
        name = ".torrent",
        value = "qBitController.torrent",
    ) && addRegistryValue(
        key = "$appPathsCapabilitiesKey\\URLAssociations",
        name = "magnet",
        value = "qBitController.magnet",
    ) && addRegistryValue(
        key = applicationsKey,
        name = "FriendlyAppName",
        value = "qBitController",
    ) && addRegistryValue(
        key = "$applicationsKey\\DefaultIcon",
        name = null,
        value = "${executable.absolutePath},0",
    ) && addRegistryValue(
        key = "$applicationsKey\\SupportedTypes",
        name = ".torrent",
        value = "",
    ) && addRegistryValue(
        key = "$applicationsKey\\shell\\open\\command",
        name = null,
        value = openCommandValue(executable),
    ) && addRegistryValue(
        key = applicationsCapabilitiesKey,
        name = "ApplicationName",
        value = "qBitController",
    ) && addRegistryValue(
        key = applicationsCapabilitiesKey,
        name = "ApplicationDescription",
        value = "qBitController",
    ) && addRegistryValue(
        key = applicationsCapabilitiesKey,
        name = "ApplicationIcon",
        value = "${executable.absolutePath},0",
    ) && addRegistryValue(
        key = "$applicationsCapabilitiesKey\\FileAssociations",
        name = ".torrent",
        value = "qBitController.torrent",
    ) && addRegistryValue(
        key = "$applicationsCapabilitiesKey\\URLAssociations",
        name = "magnet",
        value = "qBitController.magnet",
    ) && addRegistryValue(
        key = "HKCU\\Software\\RegisteredApplications",
        name = "qBitController",
        value = "Software\\qBitController\\Capabilities",
    )
}

private fun registerWindowsMachineAppCapabilities(executable: File): Boolean {
    val registryFile = File.createTempFile("qbitcontroller-default-apps-", ".reg")
    registryFile.writeWindowsRegistryFile(buildWindowsMachineAppRegistrationFile(executable))
    registryFile.deleteOnExit()

    return try {
        val command = "Start-Process -FilePath reg.exe " +
            "-ArgumentList @('import', ${powershellSingleQuotedString(registryFile.absolutePath)}) " +
            "-Verb RunAs -Wait"

        ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            command,
        ).redirectErrorStream(true).start().waitFor() == 0
    } catch (_: Exception) {
        false
    }
}

private fun buildWindowsMachineAppRegistrationFile(executable: File): String {
    val executablePath = executable.absolutePath
    val executableParent = executable.parentFile.absolutePath
    val commandValue = openCommandValue(executable)
    val iconValue = "$executablePath,0"

    return """
        Windows Registry Editor Version 5.00

        [HKEY_LOCAL_MACHINE\SOFTWARE\qBitController\Capabilities]
        "ApplicationName"="${registryString("qBitController")}"
        "ApplicationDescription"="${registryString("Open .torrent files and magnet links with qBitController.")}"
        "ApplicationIcon"="${registryString(iconValue)}"

        [HKEY_LOCAL_MACHINE\SOFTWARE\qBitController\Capabilities\FileAssociations]
        ".torrent"="qBitController.torrent"

        [HKEY_LOCAL_MACHINE\SOFTWARE\qBitController\Capabilities\URLAssociations]
        "magnet"="qBitController.magnet"

        [HKEY_LOCAL_MACHINE\SOFTWARE\RegisteredApplications]
        "qBitController"="Software\\qBitController\\Capabilities"

        [HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\qBitController.exe]
        @="${registryString(executablePath)}"
        "Path"="${registryString(executableParent)}"

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\.torrent\OpenWithProgids]
        "qBitController.torrent"=""

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\qBitController.torrent]
        @="BitTorrent File"

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\qBitController.torrent\DefaultIcon]
        @="${registryString(iconValue)}"

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\qBitController.torrent\shell\open\command]
        @="${registryString(commandValue)}"

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\qBitController.magnet]
        @="URL:Magnet Protocol"
        "URL Protocol"=""

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\qBitController.magnet\DefaultIcon]
        @="${registryString(iconValue)}"

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\qBitController.magnet\shell\open\command]
        @="${registryString(commandValue)}"

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\Applications\qBitController.exe]
        "FriendlyAppName"="qBitController"

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\Applications\qBitController.exe\DefaultIcon]
        @="${registryString(iconValue)}"

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\Applications\qBitController.exe\SupportedTypes]
        ".torrent"=""

        [HKEY_LOCAL_MACHINE\SOFTWARE\Classes\Applications\qBitController.exe\shell\open\command]
        @="${registryString(commandValue)}"
    """.trimIndent()
}

private fun isWindowsMagnetProtocolHandlerRegistered(executable: File): Boolean {
    val commandValue = openCommandValue(executable)
    val userChoiceProgId = queryRegistryValue(
        key = "HKCU\\Software\\Microsoft\\Windows\\Shell\\Associations\\UrlAssociations\\magnet\\UserChoice",
        name = "ProgId",
    )

    if (userChoiceProgId != null) {
        return isQBitControllerProgId(userChoiceProgId, expectedProgId = "qBitController.magnet") ||
            listOf(
                "HKCR\\$userChoiceProgId\\shell\\open\\command",
                "HKCU\\Software\\Classes\\$userChoiceProgId\\shell\\open\\command",
            ).any { key ->
                queryDefaultRegistryValue(key)?.equals(commandValue, ignoreCase = true) == true
            }
    }

    return listOf(
        "HKCR\\magnet\\shell\\open\\command",
        "HKCU\\Software\\Classes\\magnet\\shell\\open\\command",
        "HKCR\\qBitController.magnet\\shell\\open\\command",
        "HKCU\\Software\\Classes\\qBitController.magnet\\shell\\open\\command",
    ).filterNotNull().any { key ->
        queryDefaultRegistryValue(key)?.equals(commandValue, ignoreCase = true) == true
    }
}

private fun isWindowsTorrentFileHandlerRegistered(executable: File): Boolean {
    val commandValue = openCommandValue(executable)
    val userChoiceProgId = queryRegistryValue(
        key = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\FileExts\\.torrent\\UserChoice",
        name = "ProgId",
    )

    if (userChoiceProgId != null) {
        return isQBitControllerProgId(userChoiceProgId, expectedProgId = "qBitController.torrent") ||
            listOf(
                "HKCR\\$userChoiceProgId\\shell\\open\\command",
                "HKCU\\Software\\Classes\\$userChoiceProgId\\shell\\open\\command",
            ).any { key ->
                queryDefaultRegistryValue(key)?.equals(commandValue, ignoreCase = true) == true
            }
    }

    val progIds = listOf(
        queryDefaultRegistryValue("HKCR\\.torrent"),
        queryDefaultRegistryValue("HKCU\\Software\\Classes\\.torrent"),
    ).filterNotNull()

    return progIds.any { progId ->
        listOf(
            "HKCR\\$progId\\shell\\open\\command",
            "HKCU\\Software\\Classes\\$progId\\shell\\open\\command",
        ).any { key ->
            queryDefaultRegistryValue(key)?.equals(commandValue, ignoreCase = true) == true
        }
    }
}

private fun isQBitControllerProgId(progId: String, expectedProgId: String): Boolean =
    progId.equals(expectedProgId, ignoreCase = true) ||
        progId.equals("Applications\\qBitController.exe", ignoreCase = true)

private fun openCommandValue(executable: File) = "\"${executable.absolutePath}\" \"%1\""

private fun getPackagedExecutable(): File? {
    val command = ProcessHandle.current().info().command().orElse(null) ?: return null
    val executable = File(command)
    return executable.takeIf { it.name.equals("qBitController.exe", ignoreCase = true) }
        ?: findInstalledExecutable()
}

private fun findInstalledExecutable(): File? {
    val appData = System.getenv("LOCALAPPDATA")?.let { File(it) }
    val programFiles = System.getenv("ProgramFiles")?.let { File(it) }
    val programFilesX86 = System.getenv("ProgramFiles(x86)")?.let { File(it) }

    return listOfNotNull(
        appData?.resolve("qBitController\\qBitController.exe"),
        appData?.resolve("Programs\\qBitController\\qBitController.exe"),
        programFiles?.resolve("qBitController\\qBitController.exe"),
        programFilesX86?.resolve("qBitController\\qBitController.exe"),
    ).firstOrNull { it.isFile }
}

private fun openWindowsDefaultAppsSettingsForRegisteredApp(): Boolean =
    openWindowsSettings("ms-settings:defaultapps?registeredAppMachine=${urlEncode("qBitController")}") ||
        openWindowsSettings("ms-settings:defaultapps?registeredAppUser=${urlEncode("qBitController")}") ||
        openWindowsDefaultAppsSettings()

private fun openWindowsDefaultAppsSettings(): Boolean =
    openWindowsSettings("ms-settings:defaultapps")

private fun openWindowsSettings(uri: String): Boolean =
    try {
        ProcessBuilder("cmd", "/c", "start", "", uri)
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    } catch (_: Exception) {
        false
    }

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8)

private fun registryString(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

private fun powershellSingleQuotedString(value: String): String =
    "'${value.replace("'", "''")}'"

private fun File.writeWindowsRegistryFile(content: String) {
    outputStream().use { output ->
        output.write(byteArrayOf(0xFF.toByte(), 0xFE.toByte()))
        output.write(content.toByteArray(Charsets.UTF_16LE))
    }
}

private fun isWindowsMachineDefaultAppRegistered(): Boolean =
    queryRegistryValue(
        key = "HKLM\\SOFTWARE\\RegisteredApplications",
        name = "qBitController",
    )?.equals("Software\\qBitController\\Capabilities", ignoreCase = true) == true

private fun addRegistryValue(key: String, name: String?, value: String): Boolean {
    val command = buildList {
        addAll(listOf("reg", "add", key))
        if (name == null) {
            add("/ve")
        } else {
            addAll(listOf("/v", name))
        }
        addAll(listOf("/t", "REG_SZ", "/d", value, "/f"))
    }

    return try {
        ProcessBuilder(command).redirectErrorStream(true).start().waitFor()
            .let { exitCode -> exitCode == 0 }
    } catch (_: Exception) {
        false
    }
}

private fun queryDefaultRegistryValue(key: String): String? {
    return queryRegistryValue(key, name = null)
}

private fun queryRegistryValue(key: String, name: String?): String? {
    return try {
        val command = buildList {
            addAll(listOf("reg", "query", key))
            if (name == null) {
                add("/ve")
            } else {
                addAll(listOf("/v", name))
            }
        }
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() != 0) return null

        output.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.contains("REG_SZ") }
            ?.substringAfter("REG_SZ")
            ?.trim()
    } catch (_: Exception) {
        null
    }
}
