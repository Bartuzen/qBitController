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
}
