package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.model.TorrentFilePriority
import dev.bartuzen.qbitcontroller.network.RequestManager

class TorrentFilesRepository(
    private val requestManager: RequestManager,
) {
    suspend fun getFiles(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getFiles(hash)
    }

    suspend fun setFilePriority(serverId: Int, hash: String, ids: List<Int>, priority: TorrentFilePriority) =
        requestManager.request(serverId) { service ->
            service.setFilePriority(hash, ids.joinToString("|"), priority.id)
        }

    suspend fun renameFile(serverId: Int, hash: String, file: String, newName: String) =
        requestManager.request(serverId) { service ->
            service.renameFile(hash, file, newName)
        }

    suspend fun renameFolder(serverId: Int, hash: String, folder: String, newName: String) =
        requestManager.request(serverId) { service ->
            service.renameFolder(hash, folder, newName)
        }
}
