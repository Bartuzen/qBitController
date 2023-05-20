package dev.bartuzen.qbitcontroller.data.repositories.rss

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssFeedRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getRssFeeds(serverId: Int) = requestManager.request(serverId) { service ->
        service.getRssFeeds(false)
    }

    suspend fun refreshAllFeeds(serverId: Int) = requestManager.request(serverId) { service ->
        service.refreshItem("")
    }

    suspend fun addRssFeed(serverId: Int, url: String, path: String) = requestManager.request(serverId) { service ->
        service.addRssFeed(url, path)
    }

    suspend fun addRssFolder(serverId: Int, path: String) = requestManager.request(serverId) { service ->
        service.addRssFolder(path)
    }

    suspend fun removeItem(serverId: Int, path: String) = requestManager.request(serverId) { service ->
        service.removeItem(path)
    }

    suspend fun moveItem(serverId: Int, from: String, to: String) = requestManager.request(serverId) { service ->
        service.moveItem(from, to)
    }
}
