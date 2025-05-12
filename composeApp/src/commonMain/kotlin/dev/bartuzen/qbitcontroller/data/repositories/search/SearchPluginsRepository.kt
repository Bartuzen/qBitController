package dev.bartuzen.qbitcontroller.data.repositories.search

import dev.bartuzen.qbitcontroller.network.RequestManager

class SearchPluginsRepository(
    private val requestManager: RequestManager,
) {
    suspend fun getPlugins(serverId: Int) = requestManager.request(serverId) { service ->
        service.getPlugins()
    }

    suspend fun enablePlugins(serverId: Int, plugins: List<String>, isEnabled: Boolean) =
        requestManager.request(serverId) { service ->
            service.enablePlugins(plugins.joinToString("|"), isEnabled)
        }

    suspend fun installPlugins(serverId: Int, sources: List<String>) = requestManager.request(serverId) { service ->
        service.installPlugins(sources.joinToString("|"))
    }

    suspend fun uninstallPlugins(serverId: Int, plugins: List<String>) = requestManager.request(serverId) { service ->
        service.uninstallPlugins(plugins.joinToString("|"))
    }

    suspend fun updatePlugins(serverId: Int) = requestManager.request(serverId) { service ->
        service.updatePlugins()
    }
}
