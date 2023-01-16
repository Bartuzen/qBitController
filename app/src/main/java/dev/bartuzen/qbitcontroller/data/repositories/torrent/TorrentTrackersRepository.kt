package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentTrackersRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getTrackers(serverConfig: ServerConfig, hash: String) = requestManager.request(serverConfig) { service ->
        service.getTorrentTrackers(hash)
    }

    suspend fun addTrackers(serverConfig: ServerConfig, hash: String, urls: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.addTorrentTrackers(hash, urls.joinToString("\n"))
        }

    suspend fun deleteTrackers(serverConfig: ServerConfig, hash: String, urls: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.deleteTorrentTrackers(hash, urls.joinToString("|"))
        }

    suspend fun editTrackers(serverConfig: ServerConfig, hash: String, tracker: String, newUrl: String) =
        requestManager.request(serverConfig) { service ->
            service.editTorrentTrackers(hash, tracker, newUrl)
        }
}
