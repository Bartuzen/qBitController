package dev.bartuzen.qbitcontroller.data

import android.content.Context
import android.content.SharedPreferences
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.model.Protocol
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerManager @Inject constructor(
    @ApplicationContext context: Context,
    private val requestManager: RequestManager
) {
    private val sharedPref = context.getSharedPreferences("servers", Context.MODE_PRIVATE)

    private val mapper = jacksonObjectMapper()

    private fun getServerConfigs() = mapper.readValue<ServerConfigMap>(
        sharedPref.getString(Keys.SERVER_CONFIGS, null) ?: "{}"
    )

    private val _serversFlow = MutableStateFlow(getServerConfigs())
    val serversFlow = _serversFlow.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Keys.SERVER_CONFIGS) {
            _serversFlow.value = getServerConfigs()
        }
    }

    init {
        sharedPref.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun editServerMap(serverConfigsJson: String, block: (ServerConfigMap) -> Unit): String {
        val mapper = jacksonObjectMapper()
        val serverConfigs = mapper.readValue<ServerConfigMap>(serverConfigsJson)
        block(serverConfigs)
        return mapper.writeValueAsString(serverConfigs)
    }

    fun addServer(
        name: String?,
        protocol: Protocol,
        host: String,
        port: Int?,
        path: String?,
        username: String,
        password: String
    ) {
        val serverConfigsJson = sharedPref.getString(Keys.SERVER_CONFIGS, null) ?: "{}"
        val serverId = sharedPref.getInt(Keys.SERVER_CONFIGS, -1) + 1

        val serverConfig =
            ServerConfig(serverId, name, protocol, host, port, path, username, password)

        val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
            serverConfigs[serverId] = serverConfig
        }

        sharedPref.edit()
            .putString(Keys.SERVER_CONFIGS, newServerConfigsJson)
            .putInt(Keys.LAST_SERVER_ID, serverId)
            .apply()
    }

    fun editServer(serverConfig: ServerConfig) {
        val serverConfigsJson = sharedPref.getString("serverConfigs", null) ?: "{}"

        val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
            serverConfigs[serverConfig.id] = serverConfig
        }

        sharedPref.edit()
            .putString(Keys.SERVER_CONFIGS, newServerConfigsJson)
            .apply()

        requestManager.removeTorrentService(serverConfig)
    }

    fun removeServer(serverConfig: ServerConfig) {
        val serverConfigsJson = sharedPref.getString(Keys.SERVER_CONFIGS, null) ?: "{}"

        val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
            serverConfigs.remove(serverConfig.id)
        }

        sharedPref.edit()
            .putString(Keys.SERVER_CONFIGS, newServerConfigsJson)
            .apply()

        requestManager.removeTorrentService(serverConfig)
    }

    private object Keys {
        const val SERVER_CONFIGS = "serverConfigs"
        const val LAST_SERVER_ID = "lastServerId"
    }
}
