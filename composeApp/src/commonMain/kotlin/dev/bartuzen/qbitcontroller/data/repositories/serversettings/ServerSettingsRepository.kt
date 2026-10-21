package dev.bartuzen.qbitcontroller.data.repositories.serversettings

import dev.bartuzen.qbitcontroller.network.RequestManager
import kotlinx.serialization.json.JsonElement

class ServerSettingsRepository(
    private val requestManager: RequestManager,
) {
    suspend fun getPreferences(serverId: Int) = requestManager.request(serverId) { service ->
        service.getPreferences()
    }

    suspend fun getBuildInfo(serverId: Int) = requestManager.request(serverId) { service ->
        service.getBuildInfo()
    }

    suspend fun setPreferences(serverId: Int, preferences: Map<String, JsonElement>) =
        requestManager.request(serverId) { service ->
            service.setPreferences(preferences)
        }
}
