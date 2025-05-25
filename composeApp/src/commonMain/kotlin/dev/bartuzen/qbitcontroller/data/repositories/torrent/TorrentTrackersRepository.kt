package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.model.QBittorrentVersion
import dev.bartuzen.qbitcontroller.network.RequestManager
import io.ktor.http.encodeURLParameter

class TorrentTrackersRepository(
    private val requestManager: RequestManager,
) {
    suspend fun getTrackers(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getTorrentTrackers(hash)
    }

    suspend fun addTrackers(serverId: Int, hash: String, urls: List<String>) = requestManager.request(serverId) { service ->
        service.addTorrentTrackers(hash, urls.joinToString("\n"))
    }

    suspend fun deleteTrackers(serverId: Int, hash: String, urls: List<String>) =
        requestManager.request(serverId) { service ->
            val version = requestManager.getQBittorrentVersion(serverId)
            when {
                version >= QBittorrentVersion(5, 0, 4) -> service.deleteTorrentTrackers(
                    hash,
                    urls.joinToString("|") { it.encodeURLParameter() },
                )
                else -> service.deleteTorrentTrackers(hash, urls.joinToString("|"))
            }
        }

    suspend fun editTrackers(serverId: Int, hash: String, tracker: String, newUrl: String) =
        requestManager.request(serverId) { service ->
            service.editTorrentTrackers(hash, tracker, newUrl)
        }
}
