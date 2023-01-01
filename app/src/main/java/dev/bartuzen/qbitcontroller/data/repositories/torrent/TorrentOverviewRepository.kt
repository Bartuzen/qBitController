package dev.bartuzen.qbitcontroller.data.repositories.torrent

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentOverviewRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun getTorrent(serverConfig: ServerConfig, hash: String) = requestManager.request(serverConfig) { service ->
        service.getTorrentList(hash)
    }

    suspend fun getProperties(serverConfig: ServerConfig, hash: String) = requestManager.request(serverConfig) { service ->
        service.getTorrentProperties(hash)
    }

    suspend fun deleteTorrent(serverConfig: ServerConfig, hash: String, deleteFiles: Boolean) =
        requestManager.request(serverConfig) { service ->
            service.deleteTorrents(hash, deleteFiles)
        }

    suspend fun pauseTorrent(serverConfig: ServerConfig, hash: String) = requestManager.request(serverConfig) { service ->
        service.pauseTorrents(hash)
    }

    suspend fun resumeTorrent(serverConfig: ServerConfig, hash: String) = requestManager.request(serverConfig) { service ->
        service.resumeTorrents(hash)
    }

    suspend fun toggleSequentialDownload(serverConfig: ServerConfig, hash: String) =
        requestManager.request(serverConfig) { service ->
            service.toggleSequentialDownload(hash)
        }

    suspend fun togglePrioritizeFirstLastPiecesDownload(serverConfig: ServerConfig, hash: String) =
        requestManager.request(serverConfig) { service ->
            service.togglePrioritizeFirstLastPiecesDownload(hash)
        }

    suspend fun setAutomaticTorrentManagement(serverConfig: ServerConfig, hash: String, enable: Boolean) =
        requestManager.request(serverConfig) { service ->
            service.setAutomaticTorrentManagement(hash, enable)
        }

    suspend fun setDownloadSpeedLimit(serverConfig: ServerConfig, hash: String, limit: Int) =
        requestManager.request(serverConfig) { service ->
            service.setDownloadSpeedLimit(hash, limit)
        }

    suspend fun setUploadSpeedLimit(serverConfig: ServerConfig, hash: String, limit: Int) =
        requestManager.request(serverConfig) { service ->
            service.setUploadSpeedLimit(hash, limit)
        }

    suspend fun setForceStart(serverConfig: ServerConfig, hash: String, value: Boolean) =
        requestManager.request(serverConfig) { service ->
            service.setForceStart(hash, value)
        }

    suspend fun setSuperSeeding(serverConfig: ServerConfig, hash: String, value: Boolean) =
        requestManager.request(serverConfig) { service ->
            service.setSuperSeeding(hash, value)
        }

    suspend fun recheckTorrent(serverConfig: ServerConfig, hash: String) = requestManager.request(serverConfig) { service ->
        service.recheckTorrents(hash)
    }

    suspend fun reannounceTorrent(serverConfig: ServerConfig, hash: String) =
        requestManager.request(serverConfig) { service ->
            service.reannounceTorrents(hash)
        }

    suspend fun renameTorrent(serverConfig: ServerConfig, hash: String, name: String) =
        requestManager.request(serverConfig) { service ->
            service.renameTorrent(hash, name)
        }

    suspend fun getCategories(serverConfig: ServerConfig) = requestManager.request(serverConfig) { service ->
        service.getCategories()
    }

    suspend fun setCategory(serverConfig: ServerConfig, hash: String, category: String?) =
        requestManager.request(serverConfig) { service ->
            service.setCategory(hash, category ?: "")
        }

    suspend fun getTags(serverConfig: ServerConfig) = requestManager.request(serverConfig) { service ->
        service.getTags()
    }

    suspend fun addTags(serverConfig: ServerConfig, hash: String, tags: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.addTags(hash, tags.joinToString(","))
        }

    suspend fun removeTags(serverConfig: ServerConfig, hash: String, tags: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.removeTags(hash, tags.joinToString(","))
        }
}
