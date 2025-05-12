package dev.bartuzen.qbitcontroller.utils

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun String.toClipEntry() = ClipEntry(ClipData.newPlainText(null, this))
