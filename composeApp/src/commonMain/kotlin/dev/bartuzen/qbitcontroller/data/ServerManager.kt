package dev.bartuzen.qbitcontroller.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ServerManager(
    private val serverSettings: Settings,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val _serversFlow = MutableStateFlow(
        json.decodeFromString<List<ServerConfig>>(serverSettings.getString(Keys.ServerConfigs, "[]")),
    )
    val serversFlow = _serversFlow.asStateFlow()

    fun getServer(serverId: Int) =
        getServerOrNull(serverId) ?: throw IllegalStateException("Couldn't find server with id $serverId")

    fun getServerOrNull(serverId: Int) = serversFlow.value.find { it.id == serverId }

    suspend fun addServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigs = serversFlow.value
        val serverId = serverSettings[Keys.LastServerId, -1] + 1

        val newServerConfig = serverConfig.copy(id = serverId)
        val updatedServerConfigs = serverConfigs + newServerConfig

        serverSettings[Keys.ServerConfigs] = json.encodeToString(updatedServerConfigs)
        serverSettings[Keys.LastServerId] = serverId

        _serversFlow.value = updatedServerConfigs
        listeners.forEach { it.onServerAddedListener(newServerConfig) }
    }

    suspend fun editServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigs = serversFlow.value
        val updatedServerConfigs = serverConfigs.map {
            if (it.id == serverConfig.id) serverConfig else it
        }

        serverSettings[Keys.ServerConfigs] = json.encodeToString(updatedServerConfigs)

        _serversFlow.value = updatedServerConfigs
        listeners.forEach { it.onServerChangedListener(serverConfig) }
    }

    suspend fun removeServer(serverId: Int) = withContext(Dispatchers.IO) {
        val serverConfigs = serversFlow.value
        val serverConfig = serverConfigs.find { it.id == serverId } ?: return@withContext
        val updatedServerConfigs = serverConfigs.filter { it.id != serverId }

        serverSettings[Keys.ServerConfigs] = json.encodeToString(updatedServerConfigs)

        _serversFlow.value = updatedServerConfigs
        listeners.forEach { it.onServerRemovedListener(serverConfig) }
    }

    fun reorderServer(from: Int, to: Int) {
        _serversFlow.value = serversFlow.value.toMutableList().apply {
            add(to, removeAt(from))
        }
    }

    private val listeners = mutableListOf<ServerListener>()

    fun addServerListener(
        add: (ServerConfig) -> Unit = {},
        remove: (ServerConfig) -> Unit = {},
        change: (ServerConfig) -> Unit = {},
    ): ServerListener {
        val listener = object : ServerListener {
            override fun onServerAddedListener(serverConfig: ServerConfig) {
                add(serverConfig)
            }

            override fun onServerRemovedListener(serverConfig: ServerConfig) {
                remove(serverConfig)
            }

            override fun onServerChangedListener(serverConfig: ServerConfig) {
                change(serverConfig)
            }
        }
        listeners.add(listener)
        return listener
    }

    fun removeServerListener(serverListener: ServerListener) {
        listeners.remove(serverListener)
    }

    interface ServerListener {
        fun onServerAddedListener(serverConfig: ServerConfig)
        fun onServerRemovedListener(serverConfig: ServerConfig)
        fun onServerChangedListener(serverConfig: ServerConfig)
    }

    private object Keys {
        const val ServerConfigs = "serverConfigs"
        const val LastServerId = "lastServerId"
    }
}
