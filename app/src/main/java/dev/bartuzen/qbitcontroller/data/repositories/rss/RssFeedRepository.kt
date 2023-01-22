package dev.bartuzen.qbitcontroller.data.repositories.rss

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssFeedRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getRssFeeds(serverConfig: ServerConfig) = requestManager.request(serverConfig) { service ->
        service.getRssFeeds(false)
    }

    suspend fun addRssFeed(serverConfig: ServerConfig, url: String, path: String) =
        requestManager.request(serverConfig) { service ->
            service.addRssFeed(url, path)
        }

    suspend fun addRssFolder(serverConfig: ServerConfig, path: String) = requestManager.request(serverConfig) { service ->
        service.addRssFolder(path)
    }

    suspend fun removeItem(serverConfig: ServerConfig, path: String) = requestManager.request(serverConfig) { service ->
        service.removeItem(path)
    }
}
