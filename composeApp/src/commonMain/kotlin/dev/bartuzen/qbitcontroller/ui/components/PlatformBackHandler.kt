package dev.bartuzen.qbitcontroller.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.backhandler.BackHandler
import dev.bartuzen.qbitcontroller.utils.Platform
import dev.bartuzen.qbitcontroller.utils.currentPlatform

@Composable
fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    if (currentPlatform != Platform.Mobile.IOS) {
        BackHandler(
            enabled = enabled,
            onBack = onBack,
        )
    }
}
