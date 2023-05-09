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
}
