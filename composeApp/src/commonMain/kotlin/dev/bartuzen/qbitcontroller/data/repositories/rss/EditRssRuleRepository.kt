package dev.bartuzen.qbitcontroller.data.repositories.rss

import dev.bartuzen.qbitcontroller.network.RequestManager

class EditRssRuleRepository(
    private val requestManager: RequestManager,
) {
    suspend fun getRssRules(serverId: Int) = requestManager.request(serverId) { service ->
        service.getRssRules()
    }

    suspend fun setRule(serverId: Int, name: String, ruleDefinition: String) = requestManager.request(serverId) { service ->
        service.setRule(name, ruleDefinition)
    }

    suspend fun getCategories(serverId: Int) = requestManager.request(serverId) { service ->
        service.getCategories()
    }

    suspend fun getRssFeeds(serverId: Int) = requestManager.request(serverId) { service ->
        service.getRssFeeds()
    }
}
