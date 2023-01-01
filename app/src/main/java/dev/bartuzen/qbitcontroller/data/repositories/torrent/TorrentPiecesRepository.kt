package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentPiecesRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getPieces(serverConfig: ServerConfig, hash: String) = requestManager.request(serverConfig) { service ->
        service.getTorrentPieces(hash)
    }

    suspend fun getProperties(serverConfig: ServerConfig, hash: String) = requestManager.request(serverConfig) { service ->
        service.getTorrentProperties(hash)
    }
}
