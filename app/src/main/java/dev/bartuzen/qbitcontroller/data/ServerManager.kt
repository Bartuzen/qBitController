package dev.bartuzen.qbitcontroller.data

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private fun readServerConfigs() = json.decodeFromString<Map<Int, ServerConfig>>(
        settings.getStringOrNull(Keys.ServerConfigs) ?: "{}",
    ).toSortedMap()

    val serversFlow = settings.getStringFlow(Keys.ServerConfigs, "{}")
        .map { json.decodeFromString<Map<Int, ServerConfig>>(it).toSortedMap() }
        .stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.Eagerly, sortedMapOf())

    fun getServer(serverId: Int) =
        serversFlow.value[serverId] ?: throw IllegalStateException("Couldn't find server with id $serverId")

    fun getServerOrNull(serverId: Int) = serversFlow.value[serverId]

    suspend fun addServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigs = readServerConfigs()
        val serverId = settings[Keys.LastServerId, -1] + 1

        val newServerConfig = serverConfig.copy(id = serverId)
        serverConfigs[serverId] = newServerConfig

        settings[Keys.ServerConfigs] = json.encodeToString(serverConfigs.toMap())
        settings[Keys.LastServerId] = serverId

        listeners.forEach { it.onServerAddedListener(newServerConfig) }
    }

    suspend fun editServer(serverConfig: ServerConfig) = withContext(Dispatchers.IO) {
        val serverConfigs = readServerConfigs()
        serverConfigs[serverConfig.id] = serverConfig

        settings[Keys.ServerConfigs] = json.encodeToString(serverConfigs.toMap())

        listeners.forEach { it.onServerChangedListener(serverConfig) }
    }

    suspend fun removeServer(serverId: Int) = withContext(Dispatchers.IO) {
        val serverConfigs = readServerConfigs()
        val serverConfig = serverConfigs[serverId] ?: return@withContext
        serverConfigs.remove(serverId)

        settings[Keys.ServerConfigs] = json.encodeToString(serverConfigs.toMap())

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
