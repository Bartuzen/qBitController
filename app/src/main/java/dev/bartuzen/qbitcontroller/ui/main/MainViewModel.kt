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

    fun setCurrentServer(serverConfig: ServerConfig?) {
        state["current_server"] = serverConfig
    }

    private val serverListener = object : ServerManager.ServerListener {
        override fun onServerAddedListener(serverConfig: ServerConfig) {
            if (serversFlow.value.size == 1) {
                setCurrentServer(serverConfig)
            }
        }

        override fun onServerRemovedListener(serverConfig: ServerConfig) {
            if (currentServer.value?.id == serverConfig.id) {
                setCurrentServer(getFirstServer())
            }
        }

        override fun onServerChangedListener(serverConfig: ServerConfig) {
            if (currentServer.value?.id == serverConfig.id) {
                setCurrentServer(serverConfig)
            }
        }
    }

    private fun getFirstServer() = try {
        val serverList = serversFlow.value
        serverList[serverList.firstKey()]
    } catch (_: NoSuchElementException) {
        null
    }

    init {
        setCurrentServer(getFirstServer())

        serverManager.addServerListener(serverListener)
    }

    override fun onCleared() {
        serverManager.removeServerListener(serverListener)
    }
}
