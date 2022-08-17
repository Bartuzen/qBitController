package dev.bartuzen.qbitcontroller.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.ServerConfigMap
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.first
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settingsManager: SettingsManager,
    state: SavedStateHandle
) : ViewModel() {
    val currentServer = state.getLiveData<ServerConfig?>("current_server")

    val serverList = settingsManager.serversFlow.asLiveData()

    fun onServerListChanged(serverList: ServerConfigMap) {
        val currentServerId = currentServer.value?.id ?: -1

        if (serverList.size == 1 && currentServerId != serverList.first()?.id) {
            currentServer.value = serverList.first()
        }

        if (!serverList.containsKey(currentServerId)) {
            currentServer.value = if (serverList.size != 0) serverList.first() else null
        } else if (serverList[currentServerId] != currentServer.value) {
            currentServer.value = serverList[currentServerId]
        }
    }
}