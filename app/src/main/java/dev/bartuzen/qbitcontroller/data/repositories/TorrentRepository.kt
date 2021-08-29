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
        requestHelper.getTorrentService(serverConfig).getTorrentList(hash)

    suspend fun getFileList(serverConfig: ServerConfig, hash: String) =
        requestHelper.getTorrentService(serverConfig).getFileList(hash)
}