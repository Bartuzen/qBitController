package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentWebSeedsRepository @Inject constructor(
    private val requestManager: RequestManager,
) {
    suspend fun getWebSeeds(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getWebSeeds(hash)
    }
}
