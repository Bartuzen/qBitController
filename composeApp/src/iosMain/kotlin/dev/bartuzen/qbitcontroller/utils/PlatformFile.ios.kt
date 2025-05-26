package dev.bartuzen.qbitcontroller.utils

import io.github.vinceglb.filekit.PlatformFile

actual fun String.toPlatformFile() = PlatformFile(this)
