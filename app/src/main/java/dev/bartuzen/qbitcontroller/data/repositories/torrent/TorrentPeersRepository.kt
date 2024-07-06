package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentPeersRepository @Inject constructor(
    private val requestManager: RequestManager,
) {
    suspend fun getPeers(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getPeers(hash)
    }

    suspend fun addPeers(serverId: Int, hash: String, peers: List<String>) = requestManager.request(serverId) { service ->
        service.addPeers(hash, peers.joinToString("|"))
    }

    suspend fun banPeers(serverId: Int, peers: List<String>) = requestManager.request(serverId) { service ->
        service.banPeers(peers.joinToString("|"))
    }
}
