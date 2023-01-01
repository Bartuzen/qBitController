package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentFilePriority
import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentFilesRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getFiles(serverConfig: ServerConfig, hash: String) = requestManager.request(serverConfig) { service ->
        service.getFiles(hash)
    }

    suspend fun setFilePriority(serverConfig: ServerConfig, hash: String, ids: List<Int>, priority: TorrentFilePriority) =
        requestManager.request(serverConfig) { service ->
            service.setFilePriority(hash, ids.joinToString("|"), priority.id)
        }

    suspend fun renameFile(serverConfig: ServerConfig, hash: String, file: String, newName: String) =
        requestManager.request(serverConfig) { service ->
            service.renameFile(hash, file, newName)
        }

    suspend fun renameFolder(serverConfig: ServerConfig, hash: String, folder: String, newName: String) =
        requestManager.request(serverConfig) { service ->
            service.renameFolder(hash, folder, newName)
        }
}
