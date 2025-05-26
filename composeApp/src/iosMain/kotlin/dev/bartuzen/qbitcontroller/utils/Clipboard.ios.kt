package dev.bartuzen.qbitcontroller.utils

import androidx.compose.ui.platform.ClipEntry

actual fun String.toClipEntry() = ClipEntry.withPlainText(this)
