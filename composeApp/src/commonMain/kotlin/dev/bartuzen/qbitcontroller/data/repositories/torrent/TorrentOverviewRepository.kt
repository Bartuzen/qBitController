package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.model.QBittorrentVersion
import dev.bartuzen.qbitcontroller.network.RequestManager
import io.ktor.utils.io.ByteReadChannel

class TorrentOverviewRepository(
    private val requestManager: RequestManager,
) {
    suspend fun getTorrent(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getTorrentList(hash)
    }

    suspend fun getProperties(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getTorrentProperties(hash)
    }

    suspend fun getPieces(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.getTorrentPieces(hash)
    }

    suspend fun deleteTorrent(serverId: Int, hash: String, deleteFiles: Boolean) =
        requestManager.request(serverId) { service ->
            service.deleteTorrents(hash, deleteFiles)
        }

    suspend fun pauseTorrent(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        val version = requestManager.getQBittorrentVersion(serverId)
        when {
            version >= QBittorrentVersion(5, 0, 0) -> service.stopTorrents(hash)
            else -> service.pauseTorrents(hash)
        }
    }

    suspend fun resumeTorrent(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        val version = requestManager.getQBittorrentVersion(serverId)
        when {
            version >= QBittorrentVersion(5, 0, 0) -> service.startTorrents(hash)
            else -> service.resumeTorrents(hash)
        }
    }

    suspend fun toggleSequentialDownload(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.toggleSequentialDownload(hash)
    }

    suspend fun togglePrioritizeFirstLastPiecesDownload(serverId: Int, hash: String) =
        requestManager.request(serverId) { service ->
            service.togglePrioritizeFirstLastPiecesDownload(hash)
        }

    suspend fun setAutomaticTorrentManagement(serverId: Int, hash: String, enable: Boolean) =
        requestManager.request(serverId) { service ->
            service.setAutomaticTorrentManagement(hash, enable)
        }

    suspend fun setDownloadSpeedLimit(serverId: Int, hash: String, limit: Int) =
        requestManager.request(serverId) { service ->
            service.setDownloadSpeedLimit(hash, limit)
        }

    suspend fun setUploadSpeedLimit(serverId: Int, hash: String, limit: Int) = requestManager.request(serverId) { service ->
        service.setUploadSpeedLimit(hash, limit)
    }

    suspend fun setForceStart(serverId: Int, hash: String, value: Boolean) = requestManager.request(serverId) { service ->
        service.setForceStart(hash, value)
    }

    suspend fun setSuperSeeding(serverId: Int, hash: String, value: Boolean) = requestManager.request(serverId) { service ->
        service.setSuperSeeding(hash, value)
    }

    suspend fun recheckTorrent(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.recheckTorrents(hash)
    }

    suspend fun reannounceTorrent(serverId: Int, hash: String) = requestManager.request(serverId) { service ->
        service.reannounceTorrents(hash)
    }

    suspend fun renameTorrent(serverId: Int, hash: String, name: String) = requestManager.request(serverId) { service ->
        service.renameTorrent(hash, name)
    }

    suspend fun setLocation(serverId: Int, hash: String, location: String) = requestManager.request(serverId) { service ->
        service.setLocation(hash, location)
    }

    suspend fun setDownloadPath(serverId: Int, hash: String, path: String) = requestManager.request(serverId) { service ->
        service.setDownloadPath(hash, path)
    }

    suspend fun getCategories(serverId: Int) = requestManager.request(serverId) { service ->
        service.getCategories()
    }

    suspend fun getTags(serverId: Int) = requestManager.request(serverId) { service ->
        service.getTags()
    }

    suspend fun setCategory(serverId: Int, hash: String, category: String?) = requestManager.request(serverId) { service ->
        service.setCategory(hash, category ?: "")
    }

    suspend fun addTags(serverId: Int, hash: String, tags: List<String>) = requestManager.request(serverId) { service ->
        service.addTags(hash, tags.joinToString(","))
    }

    suspend fun removeTags(serverId: Int, hash: String, tags: List<String>) = requestManager.request(serverId) { service ->
        service.removeTags(hash, tags.joinToString(","))
    }

    suspend fun setShareLimit(
        serverId: Int,
        hash: String,
        ratioLimit: Double,
        seedingTimeLimit: Int,
        inactiveSeedingTimeLimit: Int,
    ) = requestManager.request(serverId) { service ->
        service.setShareLimit(hash, ratioLimit, seedingTimeLimit, inactiveSeedingTimeLimit)
    }

    suspend fun exportTorrent(serverId: Int, hash: String, block: suspend (ByteReadChannel) -> Unit) =
        requestManager.request(serverId) { service ->
            service.exportTorrent(hash, block)
        }
}
