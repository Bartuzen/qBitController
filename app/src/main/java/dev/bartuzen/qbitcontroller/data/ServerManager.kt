package dev.bartuzen.qbitcontroller.data

import android.annotation.SuppressLint
import android.content.Context
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPref = context.getSharedPreferences("servers", Context.MODE_PRIVATE)

    private val mapper = jacksonObjectMapper()

    private fun getServerConfigs() = mapper.readValue<ServerConfigMap>(
        sharedPref.getString(Keys.SERVER_CONFIGS, null) ?: "{}"
    )

    private val _serversFlow = MutableStateFlow(getServerConfigs())
    val serversFlow = _serversFlow.asStateFlow()

    private fun editServerMap(serverConfigsJson: String, block: (ServerConfigMap) -> Unit): String {
        val mapper = jacksonObjectMapper()
        val serverConfigs = mapper.readValue<ServerConfigMap>(serverConfigsJson)
        block(serverConfigs)
        return mapper.writeValueAsString(serverConfigs)
    }

    @SuppressLint("ApplySharedPref")
    suspend fun addServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigsJson = sharedPref.getString(Keys.SERVER_CONFIGS, null) ?: "{}"
        val serverId = sharedPref.getInt(Keys.LAST_SERVER_ID, -1) + 1

        val newServerConfig = serverConfig.copy(
            id = serverId
        )

        val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
            serverConfigs[serverId] = newServerConfig
        }

        val isSuccess = sharedPref.edit()
            .putString(Keys.SERVER_CONFIGS, newServerConfigsJson)
            .putInt(Keys.LAST_SERVER_ID, serverId)
            .commit()

        if (isSuccess) {
            _serversFlow.value = getServerConfigs()
            listeners.forEach { it.onServerAddedListener(newServerConfig) }
        }

    }

    @SuppressLint("ApplySharedPref")
    suspend fun editServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigsJson = sharedPref.getString(Keys.SERVER_CONFIGS, null) ?: "{}"

        val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
            serverConfigs[serverConfig.id] = serverConfig
        }

        val isSuccess = sharedPref.edit()
            .putString(Keys.SERVER_CONFIGS, newServerConfigsJson)
            .commit()

        if (isSuccess) {
            _serversFlow.value = getServerConfigs()
            listeners.forEach { it.onServerChangedListener(serverConfig) }
        }
    }

    @SuppressLint("ApplySharedPref")
    suspend fun removeServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigsJson = sharedPref.getString(Keys.SERVER_CONFIGS, null) ?: "{}"

        val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
            serverConfigs.remove(serverConfig.id)
        }

        val isSuccess = sharedPref.edit()
            .putString(Keys.SERVER_CONFIGS, newServerConfigsJson)
            .commit()

        if (isSuccess) {
            _serversFlow.value = getServerConfigs()
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
