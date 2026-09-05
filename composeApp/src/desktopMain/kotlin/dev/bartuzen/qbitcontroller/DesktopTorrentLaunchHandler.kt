package dev.bartuzen.qbitcontroller

import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.repositories.AddTorrentRepository
import dev.bartuzen.qbitcontroller.network.RequestResult
import java.io.File

suspend fun handleDesktopTorrentLaunch(
    args: CommandLineArguments,
    serverManager: ServerManager,
    addTorrentRepository: AddTorrentRepository,
): Boolean {
    if (!args.hasTorrentLaunch) return false

    val serverId = serverManager.serversFlow.value.firstOrNull()?.id ?: return true
    val files = args.torrentFileUris?.map { path ->
        val file = File(path)
        file.name to file.readBytes()
    }

    addTorrentRepository.addTorrent(
        serverId = serverId,
        links = args.torrentUrl?.let(::listOf),
        files = files,
        savePath = null,
        category = null,
        tags = emptyList(),
        stopCondition = null,
        contentLayout = null,
        torrentName = null,
        downloadSpeedLimit = 0,
        uploadSpeedLimit = 0,
        ratioLimit = null,
        seedingTimeLimit = null,
        isPaused = false,
        skipHashChecking = false,
        isAutoTorrentManagementEnabled = null,
        isSequentialDownloadEnabled = false,
        isFirstLastPiecePrioritized = false,
    ).also { result ->
        if (result is RequestResult.Error) {
            System.err.println("Failed to add torrent: $result")
        }
    }

    return true
}
