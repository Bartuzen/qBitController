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

    var theme
        get() = settingsManager.theme.value
        set(value) {
            settingsManager.theme.value = value
        }
}
