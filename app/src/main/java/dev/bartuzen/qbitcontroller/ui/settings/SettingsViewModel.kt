package dev.bartuzen.qbitcontroller.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {
    fun getServers() = runBlocking {
        settingsManager.serversFlow.first()
    }

    fun addServer(serverConfig: ServerConfig) = viewModelScope.launch {
        settingsManager.addServer(serverConfig)
    }

    fun editServer(serverConfig: ServerConfig) = viewModelScope.launch {
        settingsManager.editServer(serverConfig)
    }

    fun removeServer(serverConfig: ServerConfig) = viewModelScope.launch {
        settingsManager.removeServer(serverConfig)
    }
}
