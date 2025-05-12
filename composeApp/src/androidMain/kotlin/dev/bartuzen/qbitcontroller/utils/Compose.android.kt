package dev.bartuzen.qbitcontroller.utils

import android.os.Build
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
actual fun Modifier.dropdownMenuHeight() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
    val density = LocalDensity.current
    val availableHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }

    heightIn(max = availableHeight)
} else {
    this
}
