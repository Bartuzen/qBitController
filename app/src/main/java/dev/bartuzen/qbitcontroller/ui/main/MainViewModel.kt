package dev.bartuzen.qbitcontroller.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val state: SavedStateHandle,
    serverManager: ServerManager
) : ViewModel() {
    val currentServer = state.getStateFlow<ServerConfig?>("current_server", null)

    val serversFlow = serverManager.serversFlow

    fun setCurrentServer(serverConfig: ServerConfig) {
        state["current_server"] = serverConfig
    }

    init {
        viewModelScope.launch {
            serversFlow.collectLatest { serverList ->
                val currentServerId = currentServer.value?.id ?: -1
                val firstServer = try {
                    serverList[serverList.firstKey()]
                } catch (_: NoSuchElementException) {
                    null
                }

                if (currentServerId !in serverList) { // Server is removed
                    state["current_server"] = firstServer
                } else if (serverList[currentServerId] != currentServer.value) { // Server is updated
                    state["current_server"] = serverList[currentServerId]
                }
            }
        }
    }
}
