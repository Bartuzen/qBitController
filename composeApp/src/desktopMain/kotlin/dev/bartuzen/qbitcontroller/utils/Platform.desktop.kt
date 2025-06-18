package dev.bartuzen.qbitcontroller.utils

actual val currentPlatform: Platform = when {
    System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> Platform.Desktop.Windows
    System.getProperty("os.name").startsWith("Linux", ignoreCase = true) -> Platform.Desktop.Linux
    System.getProperty("os.name").startsWith("Mac", ignoreCase = true) -> Platform.Desktop.MacOS
    else -> error("Unsupported desktop platform: ${System.getProperty("os.name")}")
}
