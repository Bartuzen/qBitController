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
        serverConfig: ServerConfig, torrentHashes: List<String>, deleteFiles: Boolean
    ) = requestManager.request(serverConfig) { service ->
        service.deleteTorrents(torrentHashes.joinToString("|"), deleteFiles)
    }

    suspend fun pauseTorrents(serverConfig: ServerConfig, torrentHashes: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.pauseTorrents(torrentHashes.joinToString("|"))
        }

    suspend fun resumeTorrents(serverConfig: ServerConfig, torrentHashes: List<String>) =
        requestManager.request(serverConfig) { service ->
            service.resumeTorrents(torrentHashes.joinToString("|"))
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
}
