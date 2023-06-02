package dev.bartuzen.qbitcontroller.data.repositories.rss

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssRulesRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getRssRules(serverId: Int) = requestManager.request(serverId) { service ->
        service.getRssRules()
    }

    suspend fun createRule(serverId: Int, name: String) = requestManager.request(serverId) { service ->
        service.setRule(name, "{}")
    }

    suspend fun renameRule(serverId: Int, name: String, newName: String) = requestManager.request(serverId) { service ->
        service.renameRule(name, newName)
    }

    suspend fun deleteRule(serverId: Int, name: String) = requestManager.request(serverId) { service ->
        service.deleteRule(name)
    }
}
