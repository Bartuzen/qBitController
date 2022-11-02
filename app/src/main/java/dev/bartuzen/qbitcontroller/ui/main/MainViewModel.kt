package dev.bartuzen.qbitcontroller.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val state: SavedStateHandle,
    settingsManager: SettingsManager
) : ViewModel() {
    val currentServer = state.getStateFlow<ServerConfig?>("current_server", null)

    val serverList = settingsManager.serversFlow

    fun setCurrentServer(serverConfig: ServerConfig) {
        state["current_server"] = serverConfig
    }

    init {
        viewModelScope.launch {
            serverList.collectLatest { serverList ->
                val currentServerId = currentServer.value?.id ?: -1

                if (serverList.size == 1 && currentServerId != serverList.first()?.id) {
                    state["current_server"] = serverList.first()
                }

                if (!serverList.containsKey(currentServerId)) {
                    state["current_server"] = if (serverList.size != 0) serverList.first() else null
                } else if (serverList[currentServerId] != currentServer.value) {
                    state["current_server"] = serverList[currentServerId]
                }
            }
        }
    }
}
