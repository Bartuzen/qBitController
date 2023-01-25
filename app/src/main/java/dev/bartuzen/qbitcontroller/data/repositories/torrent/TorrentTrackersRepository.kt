package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentTrackersRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getTrackers(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getTorrentTrackers(hash)
    }

    suspend fun addTrackers(serverId: Int, hash: String, urls: List<String>) = requestManager.request(serverId) { service ->
        service.addTorrentTrackers(hash, urls.joinToString("\n"))
    }

    suspend fun deleteTrackers(serverId: Int, hash: String, urls: List<String>) =
        requestManager.request(serverId) { service ->
            service.deleteTorrentTrackers(hash, urls.joinToString("|"))
        }

    suspend fun editTrackers(serverId: Int, hash: String, tracker: String, newUrl: String) =
        requestManager.request(serverId) { service ->
            service.editTorrentTrackers(hash, tracker, newUrl)
        }
}
