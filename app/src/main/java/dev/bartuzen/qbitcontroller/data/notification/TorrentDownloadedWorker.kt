package dev.bartuzen.qbitcontroller.data.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.delay

@HiltWorker
class TorrentDownloadedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workParams: WorkerParameters,
    private val requestManager: RequestManager,
    private val serverManager: ServerManager
) : CoroutineWorker(appContext, workParams) {

    private val torrents: MutableMap<Int, List<Torrent>> = mutableMapOf()

    private val completedStates = listOf(
        TorrentState.UPLOADING,
        TorrentState.STALLED_UP,
        TorrentState.PAUSED_UP,
        TorrentState.FORCED_UP,
        TorrentState.QUEUED_UP,
        TorrentState.CHECKING_UP
    )

    private val downloadingStates = listOf(
        TorrentState.DOWNLOADING,
        TorrentState.CHECKING_DL,
        TorrentState.FORCED_META_DL,
        TorrentState.QUEUED_DL,
        TorrentState.META_DL,
        TorrentState.FORCED_DL,
        TorrentState.PAUSED_DL,
        TorrentState.STALLED_DL
    )

    override suspend fun doWork(): Result {
        while (true) {
            checkCompleted()
            delay(15 * 60 * 1000)
        }
    }

    private suspend fun checkCompleted() {
        serverManager.serversFlow.value.forEach { (serverId, serverConfig) ->
            val result = requestManager.request(serverId) { service ->
                service.getTorrentList()
            }

            if (result is RequestResult.Success) {
                val oldTorrents = torrents[serverId]
                val newTorrents = result.data

                torrents[serverId] = newTorrents

                if (oldTorrents == null) {
                    return@forEach
                }

                newTorrents.forEach { torrent ->
                    val oldTorrent = oldTorrents.find { it.hash == torrent.hash }

                    if (torrent.state in completedStates && (oldTorrent == null || oldTorrent.state in downloadingStates)) {
                        sendNotification(serverConfig, torrent)
                    }
                }
            }
        }
    }

    private fun sendNotification(serverConfig: ServerConfig, torrent: Torrent) {
        val serverId = serverConfig.id

        val notification = NotificationCompat.Builder(applicationContext, "channel_server_${serverId}_downloaded")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(applicationContext.getString(R.string.notification_torrent_downloaded, torrent.name))
            .setSubText(serverConfig.name ?: serverConfig.visibleUrl)
            .setGroup("torrent_downloaded_$serverId")
            .setSortKey(torrent.name.lowercase())
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify("torrent_downloaded_${serverConfig.id}_${torrent.hash}", 0, notification)
    }
}
