package dev.bartuzen.qbitcontroller.data.repositories

import dev.bartuzen.qbitcontroller.model.QBittorrentVersion
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.network.RequestResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class AddTorrentRepository(
    private val requestManager: RequestManager,
) {
    suspend fun addTorrent(
        serverId: Int,
        links: List<String>?,
        files: List<Pair<String, ByteArray>>?,
        savePath: String?,
        category: String?,
        tags: List<String>,
        stopCondition: String?,
        contentLayout: String?,
        torrentName: String?,
        downloadSpeedLimit: Int?,
        uploadSpeedLimit: Int?,
        ratioLimit: Double?,
        seedingTimeLimit: Int?,
        isPaused: Boolean,
        skipHashChecking: Boolean,
        isAutoTorrentManagementEnabled: Boolean?,
        isSequentialDownloadEnabled: Boolean,
        isFirstLastPiecePrioritized: Boolean,
    ): RequestResult<String> {
        val fileParts = files?.map { (fileName, byteArray) ->
            MultipartBody.Part.createFormData(
                "torrents",
                fileName,
                byteArray.toRequestBody("application/x-bittorrent".toMediaTypeOrNull()),
            )
        }

        val version = requestManager.getQBittorrentVersion(serverId)
        val pausedKey = when {
            version >= QBittorrentVersion(5, 0, 0) -> "stopped"
            else -> "paused"
        }
        val pausedPart = MultipartBody.Part.createFormData(pausedKey, isPaused.toString())
        val parts = fileParts.orEmpty() + pausedPart

        return requestManager.request(serverId) { service ->
            service.addTorrent(
                links?.joinToString("\n"),
                parts,
                savePath,
                category,
                tags.joinToString(",").ifEmpty { null },
                stopCondition,
                contentLayout,
                torrentName,
                downloadSpeedLimit,
                uploadSpeedLimit,
                ratioLimit,
                seedingTimeLimit,
                skipHashChecking,
                isAutoTorrentManagementEnabled,
                isSequentialDownloadEnabled,
                isFirstLastPiecePrioritized,
            )
        }
    }

    suspend fun getCategories(serverId: Int) = requestManager.request(serverId) { service ->
        service.getCategories()
    }

    suspend fun getTags(serverId: Int) = requestManager.request(serverId) { service ->
        service.getTags()
    }

    suspend fun getDefaultSavePath(serverId: Int) = requestManager.request(serverId) { service ->
        service.getDefaultSavePath()
    }
}
