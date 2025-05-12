package dev.bartuzen.qbitcontroller.ui.settings.network

import androidx.lifecycle.ViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager

class NetworkSettingsViewModel(
    settingsManager: SettingsManager,
) : ViewModel() {
    val connectionTimeout = settingsManager.connectionTimeout
    var autoRefreshInterval = settingsManager.autoRefreshInterval
}
