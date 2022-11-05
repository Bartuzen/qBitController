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
        serverConfig: ServerConfig, hashes: List<String>, deleteFiles: Boolean
    ) = requestManager.request(serverConfig) { service ->
        service.deleteTorrents(hashes.joinToString("|"), deleteFiles)
    }

    suspend fun pauseTorrents(serverConfig: ServerConfig, hashes: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.pauseTorrents(hashes.joinToString("|"))
        }

    suspend fun resumeTorrents(serverConfig: ServerConfig, hashes: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.resumeTorrents(hashes.joinToString("|"))
        }

    suspend fun getCategories(serverConfig: ServerConfig) =
        requestManager.request(serverConfig) { service ->
            service.getCategories()
        }

    suspend fun getTags(serverConfig: ServerConfig) =
        requestManager.request(serverConfig) { service ->
            service.getTags()
        }

    suspend fun deleteCategory(serverConfig: ServerConfig, category: String) =
        requestManager.request(serverConfig) { service ->
            service.deleteCategories(category)
        }

    suspend fun deleteTag(serverConfig: ServerConfig, tag: String) =
        requestManager.request(serverConfig) { service ->
            service.deleteTags(tag)
        }

    suspend fun increaseTorrentPriority(serverConfig: ServerConfig, hashes: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.increaseTorrentPriority(hashes.joinToString("|"))
        }

    suspend fun decreaseTorrentPriority(serverConfig: ServerConfig, hashes: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.decreaseTorrentPriority(hashes.joinToString("|"))
        }

    suspend fun maximizeTorrentPriority(serverConfig: ServerConfig, hashes: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.maximizeTorrentPriority(hashes.joinToString("|"))
        }

    suspend fun minimizeTorrentPriority(serverConfig: ServerConfig, hashes: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.minimizeTorrentPriority(hashes.joinToString("|"))
        }
}
