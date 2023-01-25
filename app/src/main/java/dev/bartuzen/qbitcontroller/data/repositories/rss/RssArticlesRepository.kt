package dev.bartuzen.qbitcontroller.data.repositories.rss

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssArticlesRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getRssFeeds(serverId: Int) = requestManager.request(serverId) { service ->
        service.getRssFeeds(true)
    }

    suspend fun refreshItem(serverId: Int, feedPath: List<String>) = requestManager.request(serverId) { service ->
        service.refreshItem(feedPath.joinToString("\\"))
    }
}
