package dev.bartuzen.qbitcontroller.ui.settings.network

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import javax.inject.Inject

@HiltViewModel
class NetworkSettingsViewModel @Inject constructor(
    settingsManager: SettingsManager,
) : ViewModel() {
    val connectionTimeout = settingsManager.connectionTimeout
    var autoRefreshInterval = settingsManager.autoRefreshInterval
}
