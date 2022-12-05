package dev.bartuzen.qbitcontroller.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.model.Protocol
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.utils.sharedpreferences.enumPreference
import dev.bartuzen.qbitcontroller.utils.sharedpreferences.primitivePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val requestManager: RequestManager
) {
    private val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val mapper = jacksonObjectMapper()

    private fun getServerConfigs() = mapper.readValue<ServerConfigMap>(
        sharedPref.getString("serverConfigs", "{}") ?: "{}"
    )

    private val _serversFlow = MutableStateFlow(getServerConfigs())
    val serversFlow = _serversFlow.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "serverConfigs") {
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
        val serverConfigsJson = sharedPref.getString("serverConfigs", "{}") ?: "{}"
        val serverId = sharedPref.getInt("lastServerId", -1) + 1

        val serverConfig =
            ServerConfig(serverId, name, protocol, host, port, path, username, password)

        val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
            serverConfigs[serverId] = serverConfig
        }

        sharedPref.edit()
            .putString("serverConfigs", newServerConfigsJson)
            .putInt("lastServerId", serverId)
            .apply()
    }

    fun editServer(serverConfig: ServerConfig) {
        val serverConfigsJson = sharedPref.getString("serverConfigs", "{}") ?: "{}"

        val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
            serverConfigs[serverConfig.id] = serverConfig
        }

        sharedPref.edit()
            .putString("serverConfigs", newServerConfigsJson)
            .apply()

        requestManager.removeTorrentService(serverConfig)
    }

    fun removeServer(serverConfig: ServerConfig) {
        val serverConfigsJson = sharedPref.getString("serverConfigs", "{}") ?: "{}"

        val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
            serverConfigs.remove(serverConfig.id)
        }

        sharedPref.edit()
            .putString("serverConfigs", newServerConfigsJson)
            .apply()

        requestManager.removeTorrentService(serverConfig)
    }

    val theme = enumPreference(sharedPref, "theme", Theme.SYSTEM_DEFAULT, Theme::valueOf)
    val sort = enumPreference(sharedPref, "sort", TorrentSort.NAME, TorrentSort::valueOf)
    val isReverseSorting = primitivePreference(sharedPref, "isReverseSorting", false)
}

typealias ServerConfigMap = SortedMap<Int, ServerConfig>

enum class Theme {
    LIGHT, DARK, SYSTEM_DEFAULT
}

enum class TorrentSort {
    @JsonProperty("name")
    NAME,

    @JsonProperty("hash")
    HASH,

    @JsonProperty("dlspeed")
    DOWNLOAD_SPEED,

    @JsonProperty("upspeed")
    UPLOAD_SPEED,

    @JsonProperty("priority")
    PRIORITY
}

fun Theme.toDelegate() = when (this) {
    Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    Theme.SYSTEM_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}
