package dev.bartuzen.qbitcontroller.data

import com.russhwolf.settings.Settings
import dev.bartuzen.qbitcontroller.model.WindowState

class DesktopSettingsManager(
    settings: Settings,
) : SettingsManager(settings) {
    val windowState = jsonPreference(settings, "windowState", WindowState())
}
