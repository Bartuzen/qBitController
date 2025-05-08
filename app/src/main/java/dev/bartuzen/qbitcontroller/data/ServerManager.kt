package dev.bartuzen.qbitcontroller.data

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val settings = SharedPreferencesSettings(context.getSharedPreferences("servers", Context.MODE_PRIVATE))

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val _serversFlow = MutableStateFlow(
        json.decodeFromString<Map<Int, ServerConfig>>(settings.getString(Keys.ServerConfigs, "{}")).toSortedMap(),
    )
    val serversFlow = _serversFlow.asStateFlow()

    fun getServer(serverId: Int) =
        serversFlow.value[serverId] ?: throw IllegalStateException("Couldn't find server with id $serverId")

    fun getServerOrNull(serverId: Int) = serversFlow.value[serverId]

    suspend fun addServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigs = serversFlow.value
        val serverId = settings[Keys.LastServerId, -1] + 1

        val newServerConfig = serverConfig.copy(id = serverId)
        serverConfigs[serverId] = newServerConfig

        settings[Keys.ServerConfigs] = json.encodeToString(serverConfigs.toMap())
        settings[Keys.LastServerId] = serverId

        _serversFlow.value = serverConfigs
        listeners.forEach { it.onServerAddedListener(newServerConfig) }
    }

    suspend fun editServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigs = serversFlow.value
        serverConfigs[serverConfig.id] = serverConfig

        settings[Keys.ServerConfigs] = json.encodeToString(serverConfigs.toMap())

        _serversFlow.value = serverConfigs
        listeners.forEach { it.onServerChangedListener(serverConfig) }
    }

    suspend fun removeServer(serverId: Int) = withContext(Dispatchers.IO) {
        val serverConfigs = serversFlow.value
        val serverConfig = serverConfigs[serverId] ?: return@withContext
        serverConfigs.remove(serverId)

        settings[Keys.ServerConfigs] = json.encodeToString(serverConfigs.toMap())

        _serversFlow.value = serverConfigs
        listeners.forEach { it.onServerRemovedListener(serverConfig) }
    }

    private val listeners = mutableListOf<ServerListener>()

    fun addServerListener(serverListener: ServerListener) {
        listeners.add(serverListener)
    }

    fun removeServerListener(serverListener: ServerListener) {
        listeners.add(serverListener)
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
