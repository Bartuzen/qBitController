package dev.bartuzen.qbitcontroller.data.repositories

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddTorrentRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun createTorrent(
        serverConfig: ServerConfig,
        links: List<String>,
        isPaused: Boolean,
        skipHashChecking: Boolean,
        isAutoTorrentManagementEnabled: Boolean,
        isSequentialDownloadEnabled: Boolean,
        isFirstLastPiecePrioritized: Boolean
    ) = requestManager.request(serverConfig) { service ->
        service.addTorrent(
            links.joinToString("\n"),
            isPaused,
            skipHashChecking,
            isAutoTorrentManagementEnabled,
            isSequentialDownloadEnabled,
            isFirstLastPiecePrioritized
        )
    }
}
