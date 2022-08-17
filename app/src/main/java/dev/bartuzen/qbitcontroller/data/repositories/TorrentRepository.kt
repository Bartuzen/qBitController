package dev.bartuzen.qbitcontroller.data.repositories

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentRepository @Inject constructor(
    private val requestHelper: RequestHelper
) {
    suspend fun getTorrent(serverConfig: ServerConfig, hash: String) =
        requestHelper.request(serverConfig) { service ->
            service.getTorrentList(hash)
        }

    suspend fun getFiles(serverConfig: ServerConfig, hash: String) =
        requestHelper.request(serverConfig) { service ->
            service.getFiles(hash)
        }

    suspend fun getPieces(serverConfig: ServerConfig, hash: String) =
        requestHelper.request(serverConfig) { service ->
            service.getTorrentPieces(hash)
        }

    suspend fun getProperties(serverConfig: ServerConfig, hash: String) =
        requestHelper.request(serverConfig) { service ->
            service.getTorrentProperties(hash)
        }

    suspend fun pauseTorrent(serverConfig: ServerConfig, hash: String) =
        requestHelper.request(serverConfig) { service ->
            service.pauseTorrent(hash)
        }

    suspend fun resumeTorrent(serverConfig: ServerConfig, hash: String) =
        requestHelper.request(serverConfig) { service ->
            service.resumeTorrent(hash)
        }
}