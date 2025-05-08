package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.network.RequestManager

class TorrentWebSeedsRepository(
    private val requestManager: RequestManager,
) {
    suspend fun getWebSeeds(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getWebSeeds(hash)
    }
}
