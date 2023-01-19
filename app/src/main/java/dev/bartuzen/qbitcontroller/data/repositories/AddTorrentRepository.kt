package dev.bartuzen.qbitcontroller.data.repositories

import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.network.RequestResult
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddTorrentRepository @Inject constructor(
    private val requestManager: RequestManager
) {
    suspend fun createTorrent(
        serverConfig: ServerConfig,
        links: List<String>?,
        fileBytes: ByteArray?,
        savePath: String?,
        category: String?,
        tags: List<String>,
        torrentName: String?,
        downloadSpeedLimit: Int?,
        uploadSpeedLimit: Int?,
        ratioLimit: Double?,
        seedingTimeLimit: Int?,
        isPaused: Boolean,
        skipHashChecking: Boolean,
        isAutoTorrentManagementEnabled: Boolean?,
        isSequentialDownloadEnabled: Boolean,
        isFirstLastPiecePrioritized: Boolean
    ): RequestResult<Unit> {
        val filePart = if (fileBytes != null) {
            MultipartBody.Part.createFormData(
                "filename",
                "torrent",
                RequestBody.create(MediaType.parse("application/x-bittorrent"), fileBytes)
            )
        } else {
            null
        }

        return requestManager.request(serverConfig) { service ->
            service.addTorrent(
                links?.joinToString("\n"),
                filePart,
                if (isAutoTorrentManagementEnabled == true) savePath else null,
                category,
                tags.joinToString(",").ifEmpty { null },
                torrentName,
                downloadSpeedLimit,
                uploadSpeedLimit,
                ratioLimit,
                seedingTimeLimit,
                isPaused,
                skipHashChecking,
                isAutoTorrentManagementEnabled,
                isSequentialDownloadEnabled,
                isFirstLastPiecePrioritized
            )
        }
    }

    suspend fun getCategories(serverConfig: ServerConfig) = requestManager.request(serverConfig) { service ->
        service.getCategories()
    }

    suspend fun getTags(serverConfig: ServerConfig) = requestManager.request(serverConfig) { service ->
        service.getTags()
    }
}
