package dev.bartuzen.qbitcontroller.data.repositories

import dev.bartuzen.qbitcontroller.model.QBittorrentVersion
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.network.RequestResult
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully

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
        val version = requestManager.getQBittorrentVersion(serverId)
        val pausedKey = when {
            version >= QBittorrentVersion(5, 0, 0) -> "stopped"
            else -> "paused"
        }

        val multipart = MultiPartFormDataContent(
            formData {
                files?.forEach { (fileName, byteArray) ->
                    append(
                        "torrents",
                        InputProvider { buildPacket { writeFully(byteArray) } },
                        Headers.build {
                            append("Content-Type", "application/x-bittorrent")
                            append("Content-Disposition", "form-data; name=\"torrents\"; filename=\"$fileName\"")
                        },
                    )
                }

                links?.joinToString("\n")?.let { append("urls", it) }

                append(pausedKey, isPaused.toString())
                append("skip_checking", skipHashChecking.toString())
                append("sequentialDownload", isSequentialDownloadEnabled.toString())
                append("firstLastPiecePrio", isFirstLastPiecePrioritized.toString())

                savePath?.let { append("savepath", it) }
                category?.let { append("category", it) }
                if (tags.isNotEmpty()) {
                    append("tags", tags.joinToString(","))
                }
                stopCondition?.let { append("stopCondition", it) }
                contentLayout?.let { append("contentLayout", it) }
                torrentName?.let { append("rename", it) }
                downloadSpeedLimit?.let { append("dlLimit", it.toString()) }
                uploadSpeedLimit?.let { append("upLimit", it.toString()) }
                ratioLimit?.let { append("ratioLimit", it.toString()) }
                seedingTimeLimit?.let { append("seedingTimeLimit", it.toString()) }
                isAutoTorrentManagementEnabled?.let { append("autoTMM", it.toString()) }
            },
        )

        return requestManager.request(serverId) { service ->
            service.addTorrent(multipart)
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
