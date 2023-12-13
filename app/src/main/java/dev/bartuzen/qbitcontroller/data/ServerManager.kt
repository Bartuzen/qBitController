package dev.bartuzen.qbitcontroller.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPref = context.getSharedPreferences("servers", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private fun readServerConfigs() = json.decodeFromString<Map<Int, ServerConfig>>(
        sharedPref.getString(Keys.SERVER_CONFIGS, null) ?: "{}"
    ).toSortedMap()

    private val _serversFlow = MutableStateFlow(readServerConfigs())
    val serversFlow = _serversFlow.asStateFlow()

    fun getServer(serverId: Int) =
        serversFlow.value[serverId] ?: throw IllegalStateException("Couldn't find server with id $serverId")

    fun getServerOrNull(serverId: Int) = serversFlow.value[serverId]

    suspend fun addServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigs = readServerConfigs()
        val serverId = sharedPref.getInt(Keys.LAST_SERVER_ID, -1) + 1

        val newServerConfig = serverConfig.copy(
            id = serverId
        )
        serverConfigs[serverId] = newServerConfig

        val isSuccess = sharedPref.edit()
            .putString(Keys.SERVER_CONFIGS, json.encodeToString(serverConfigs.toMutableMap()))
            .putInt(Keys.LAST_SERVER_ID, serverId)
            .commit()

        if (isSuccess) {
            _serversFlow.value = serverConfigs
            listeners.forEach { it.onServerAddedListener(newServerConfig) }
        }
    }

    suspend fun editServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigs = readServerConfigs()
        serverConfigs[serverConfig.id] = serverConfig

        val isSuccess = sharedPref.edit()
            .putString(Keys.SERVER_CONFIGS, json.encodeToString(serverConfigs.toMutableMap()))
            .commit()

        if (isSuccess) {
            _serversFlow.value = serverConfigs
            listeners.forEach { it.onServerChangedListener(serverConfig) }
        }
    }

    suspend fun removeServer(serverId: Int) = withContext(Dispatchers.IO) {
        val serverConfigs = readServerConfigs()
        val serverConfig = serverConfigs[serverId] ?: return@withContext
        serverConfigs.remove(serverId)

        val isSuccess = sharedPref.edit()
            .putString(Keys.SERVER_CONFIGS, json.encodeToString(serverConfigs.toMutableMap()))
            .commit()

        if (isSuccess) {
            _serversFlow.value = serverConfigs
            listeners.forEach { it.onServerRemovedListener(serverConfig) }
        }
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
        const val SERVER_CONFIGS = "serverConfigs"
        const val LAST_SERVER_ID = "lastServerId"
    }
}
