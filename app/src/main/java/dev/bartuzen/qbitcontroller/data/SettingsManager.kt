package dev.bartuzen.qbitcontroller.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val requestManager: RequestManager
) {
    private val dataStore = context.dataStore

    val themeFlow = getFromDataStore { settings ->
        Theme.valueOf(settings[PreferenceKeys.THEME] ?: Theme.SYSTEM_DEFAULT.name)
    }

    val sortFlow = getFromDataStore { settings ->
        TorrentSort.valueOf(settings[PreferenceKeys.TORRENT_SORT] ?: TorrentSort.NAME.name)
    }

    val serversFlow = getFromDataStore { settings ->
        val serverConfigsJson =
            settings[PreferenceKeys.SERVER_CONFIGS] ?: return@getFromDataStore sortedMapOf()

        val mapper = jacksonObjectMapper()
        mapper.readValue<ServerConfigMap>(serverConfigsJson)
    }

    private fun <T> getFromDataStore(block: (Preferences) -> T) =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { settings ->
                block(settings)
            }

    private fun editServerMap(serverConfigsJson: String, block: (ServerConfigMap) -> Unit): String {
        val mapper = jacksonObjectMapper()
        val serverConfigs = mapper.readValue<ServerConfigMap>(serverConfigsJson)
        block(serverConfigs)
        return mapper.writeValueAsString(serverConfigs)
    }

    suspend fun addServer(serverConfig: ServerConfig) {
        dataStore.edit { settings ->
            val serverConfigsJson = settings[PreferenceKeys.SERVER_CONFIGS] ?: "{}"
            val serverId = (settings[PreferenceKeys.LAST_SERVER_ID] ?: 0) + 1

            serverConfig.id = serverId

            val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
                serverConfigs[serverId] = serverConfig
            }

            settings[PreferenceKeys.LAST_SERVER_ID] = serverId
            settings[PreferenceKeys.SERVER_CONFIGS] = newServerConfigsJson
        }
    }

    suspend fun editServer(serverConfig: ServerConfig) {
        dataStore.edit { settings ->
            val serverConfigsJson = settings[PreferenceKeys.SERVER_CONFIGS] ?: "{}"

            val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
                serverConfigs[serverConfig.id] = serverConfig
            }

            settings[PreferenceKeys.SERVER_CONFIGS] = newServerConfigsJson
        }
        requestManager.removeTorrentService(serverConfig)
    }

    suspend fun removeServer(serverConfig: ServerConfig) {
        dataStore.edit { settings ->
            val serverConfigsJson = settings[PreferenceKeys.SERVER_CONFIGS] ?: return@edit

            val newServerConfigsJson = editServerMap(serverConfigsJson) { serverConfigs ->
                serverConfigs.remove(serverConfig.id)
            }

            settings[PreferenceKeys.SERVER_CONFIGS] = newServerConfigsJson
        }
        requestManager.removeTorrentService(serverConfig)
    }

    suspend fun setTorrentSort(torrentSort: TorrentSort) {
        dataStore.edit { settings ->
            settings[PreferenceKeys.TORRENT_SORT] = torrentSort.name
        }
    }

    private object PreferenceKeys {
        val THEME = stringPreferencesKey("theme")
        val SERVER_CONFIGS = stringPreferencesKey("server_configs")
        val LAST_SERVER_ID = intPreferencesKey("last_server_id")
        val TORRENT_SORT = stringPreferencesKey("sort")
    }
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
