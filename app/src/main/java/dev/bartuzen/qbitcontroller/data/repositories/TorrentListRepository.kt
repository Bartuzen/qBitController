package dev.bartuzen.qbitcontroller.data.repositories

import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentListRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getTorrentList(serverConfig: ServerConfig, torrentSort: TorrentSort) =
        requestManager.request(serverConfig) { service ->
            service.getTorrentList(torrentSort = torrentSort)
        }

    suspend fun deleteTorrents(
        serverConfig: ServerConfig,
        torrentHashes: String,
        deleteFiles: Boolean
    ) = requestManager.request(serverConfig) { service ->
        service.deleteTorrents(torrentHashes, deleteFiles)
    }

    suspend fun pauseTorrents(serverConfig: ServerConfig, torrentHashes: String) =
        requestManager.request(serverConfig) { service ->
            service.pauseTorrents(torrentHashes)
        }

    suspend fun resumeTorrents(serverConfig: ServerConfig, torrentHashes: String) =
        requestManager.request(serverConfig) { service ->
            service.resumeTorrents(torrentHashes)
        }
}
