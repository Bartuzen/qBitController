package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentTagsRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getTags(serverId: Int) = requestManager.request(serverId) { service ->
        service.getTags()
    }
}
