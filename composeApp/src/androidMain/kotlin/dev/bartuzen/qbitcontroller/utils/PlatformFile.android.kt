package dev.bartuzen.qbitcontroller.utils

import androidx.core.net.toUri
import io.github.vinceglb.filekit.PlatformFile

actual fun String.toPlatformFile() = PlatformFile(toUri())
