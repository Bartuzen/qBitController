package dev.bartuzen.qbitcontroller.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverManager: ServerManager,
    private val settingsManager: SettingsManager
) : ViewModel() {
    fun getServers() = serverManager.serversFlow.value

    var connectionTimeout
        get() = settingsManager.connectionTimeout.value
        set(value) {
            settingsManager.connectionTimeout.value = value
        }

    var autoRefreshInterval
        get() = settingsManager.autoRefreshInterval.value
        set(value) {
            settingsManager.autoRefreshInterval.value = value
        }

    var autoRefreshIntervalFlow = settingsManager.autoRefreshInterval.flow

    var autoRefreshHideLoadingBar
        get() = settingsManager.autoRefreshHideLoadingBar.value
        set(value) {
            settingsManager.autoRefreshHideLoadingBar.value = value
        }

    var notificationCheckInterval
        get() = settingsManager.notificationCheckInterval.value
        set(value) {
            settingsManager.notificationCheckInterval.value = value
        }

    var theme
        get() = settingsManager.theme.value
        set(value) {
            settingsManager.theme.value = value
        }

    var pin
        get() = settingsManager.pin.value
        set(value) {
            settingsManager.pin.value = value
        }
}
