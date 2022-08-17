package dev.bartuzen.qbitcontroller.data.repositories

import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentListRepository @Inject constructor(
    private val requestHelper: RequestHelper
) {
    suspend fun getTorrentList(serverConfig: ServerConfig, torrentSort: TorrentSort) =
        requestHelper.request(serverConfig) { service ->
            service.getTorrentList(
                torrentSort = torrentSort
            )
        }
}