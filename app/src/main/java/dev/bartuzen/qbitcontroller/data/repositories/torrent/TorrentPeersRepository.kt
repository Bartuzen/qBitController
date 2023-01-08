package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentPeersRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getPeers(serverConfig: ServerConfig, hash: String) = requestManager.request(serverConfig) { service ->
        service.getPeers(hash)
    }

    suspend fun banPeers(serverConfig: ServerConfig, peers: List<String>) = requestManager.request(serverConfig) { service ->
        service.banPeers(peers.joinToString("|"))
    }
}
