package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentTagsRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getTags(serverConfig: ServerConfig) = requestManager.request(serverConfig) { service ->
        service.getTags()
    }
}
