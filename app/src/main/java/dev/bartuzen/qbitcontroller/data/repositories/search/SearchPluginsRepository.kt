package dev.bartuzen.qbitcontroller.data.repositories.search

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchPluginsRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getPlugins(serverId: Int) = requestManager.request(serverId) { service ->
        service.getPlugins()
    }

    suspend fun enablePlugins(serverId: Int, plugins: List<String>, isEnabled: Boolean) =
        requestManager.request(serverId) { service ->
            service.enablePlugins(plugins.joinToString("|"), isEnabled)
        }

    suspend fun uninstallPlugins(serverId: Int, plugins: List<String>) = requestManager.request(serverId) { service ->
        service.uninstallPlugins(plugins.joinToString("|"))
    }

    suspend fun updatePlugins(serverId: Int) = requestManager.request(serverId) { service ->
        service.updatePlugins()
    }
}
