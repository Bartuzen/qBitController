package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentCategoryRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getCategories(serverConfig: ServerConfig) = requestManager.request(serverConfig) { service ->
        service.getCategories()
    }
}
