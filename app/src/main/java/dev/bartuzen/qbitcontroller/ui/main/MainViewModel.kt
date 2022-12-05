package dev.bartuzen.qbitcontroller.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val state: SavedStateHandle,
    private val serverManager: ServerManager
) : ViewModel() {
    val currentServer = state.getStateFlow<ServerConfig?>("current_server", null)

    val serversFlow = serverManager.serversFlow

    fun setCurrentServer(serverConfig: ServerConfig) {
        state["current_server"] = serverConfig
    }

    private val serverListener: ServerManager.ServerListener

    init {
        val firstServer = try {
            val serverList = serversFlow.value
            serverList[serverList.firstKey()]
        } catch (_: NoSuchElementException) {
            null
        }
        state["current_server"] = firstServer

        serverListener = object : ServerManager.ServerListener {
            override fun onServerAddedListener(serverConfig: ServerConfig) {
                if (serversFlow.value.size == 1) {
                    state["current_server"] = serverConfig
                }
            }

            override fun onServerRemovedListener(serverConfig: ServerConfig) {
                if (serversFlow.value.size == 0) {
                    state["current_server"] = null
                }
            }

            override fun onServerChangedListener(serverConfig: ServerConfig) {
                if (currentServer.value?.id != serverConfig.id) {
                    state["current_server"] = serverConfig
                }
            }
        }

        serverManager.addServerListener(serverListener)
    }

    override fun onCleared() {
        serverManager.removeServerListener(serverListener)
    }
}
