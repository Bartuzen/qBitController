package dev.bartuzen.qbitcontroller.data.repositories

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentListRepository @Inject constructor(
    private val requestManager: RequestManager,
) {
    suspend fun getMainData(serverId: Int) = requestManager.request(serverId) { service ->
        service.getMainData()
    }

    suspend fun deleteTorrents(serverId: Int, hashes: List<String>, deleteFiles: Boolean) =
        requestManager.request(serverId) { service ->
            service.deleteTorrents(hashes.joinToString("|"), deleteFiles)
        }

    suspend fun pauseTorrents(serverId: Int, hashes: List<String>) = requestManager.request(serverId) { service ->
        service.pauseTorrents(hashes.joinToString("|"))
    }

    suspend fun resumeTorrents(serverId: Int, hashes: List<String>) = requestManager.request(serverId) { service ->
        service.resumeTorrents(hashes.joinToString("|"))
    }

    suspend fun deleteCategory(serverId: Int, category: String) = requestManager.request(serverId) { service ->
        service.deleteCategories(category)
    }

    suspend fun deleteTag(serverId: Int, tag: String) = requestManager.request(serverId) { service ->
        service.deleteTags(tag)
    }

    suspend fun increaseTorrentPriority(serverId: Int, hashes: List<String>) = requestManager.request(serverId) { service ->
        service.increaseTorrentPriority(hashes.joinToString("|"))
    }

    suspend fun decreaseTorrentPriority(serverId: Int, hashes: List<String>) = requestManager.request(serverId) { service ->
        service.decreaseTorrentPriority(hashes.joinToString("|"))
    }

    suspend fun maximizeTorrentPriority(serverId: Int, hashes: List<String>) = requestManager.request(serverId) { service ->
        service.maximizeTorrentPriority(hashes.joinToString("|"))
    }

    suspend fun minimizeTorrentPriority(serverId: Int, hashes: List<String>) = requestManager.request(serverId) { service ->
        service.minimizeTorrentPriority(hashes.joinToString("|"))
    }

    suspend fun createCategory(
        serverId: Int,
        name: String,
        savePath: String,
        downloadPathEnabled: Boolean?,
        downloadPath: String,
    ) = requestManager.request(serverId) { service ->
        service.createCategory(name, savePath, downloadPathEnabled, downloadPath)
    }

    suspend fun setLocation(serverId: Int, hashes: List<String>, location: String) =
        requestManager.request(serverId) { service ->
            service.setLocation(hashes.joinToString("|"), location)
        }

    suspend fun editCategory(
        serverId: Int,
        name: String,
        savePath: String,
        downloadPathEnabled: Boolean?,
        downloadPath: String,
    ) = requestManager.request(serverId) { service ->
        service.editCategory(name, savePath, downloadPathEnabled, downloadPath)
    }

    suspend fun createTags(serverId: Int, names: List<String>) = requestManager.request(serverId) { service ->
        service.createTags(names.joinToString(","))
    }

    suspend fun toggleSpeedLimitsMode(serverId: Int) = requestManager.request(serverId) { service ->
        service.toggleSpeedLimitsMode()
    }

    suspend fun setDownloadSpeedLimit(serverId: Int, limit: Int) = requestManager.request(serverId) { service ->
        service.setDownloadSpeedLimit(limit)
    }

    suspend fun setUploadSpeedLimit(serverId: Int, limit: Int) = requestManager.request(serverId) { service ->
        service.setUploadSpeedLimit(limit)
    }

    suspend fun shutdown(serverId: Int) = requestManager.request(serverId) { service ->
        service.shutdown()
    }

    suspend fun setCategory(serverId: Int, hashes: List<String>, category: String?) =
        requestManager.request(serverId) { service ->
            service.setCategory(hashes.joinToString("|"), category ?: "")
        }
}
