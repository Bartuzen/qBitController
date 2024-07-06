package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentPiecesRepository @Inject constructor(
    private val requestManager: RequestManager,
) {
    suspend fun getPieces(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getTorrentPieces(hash)
    }

    suspend fun getProperties(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getTorrentProperties(hash)
    }
}
