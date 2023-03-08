package dev.bartuzen.qbitcontroller.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.minutes

@HiltWorker
class TorrentDownloadedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workParams: WorkerParameters,
    private val requestManager: RequestManager,
    private val serverManager: ServerManager,
    private val settingsManager: SettingsManager
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

    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        settingsManager.notificationCheckInterval.flow.collectLatest { interval ->
            while (true) {
                checkCompleted()
                delay(interval.minutes)
            }
        }
        return Result.success()
    }

    private suspend fun checkCompleted() {
        if (!areNotificationsEnabled()) {
            return
        }

        serverManager.serversFlow.value.forEach { (serverId, serverConfig) ->
            if (!isNotificationChannelEnabled("channel_server_${serverId}_downloaded")) {
                return@forEach
            }

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

        val torrentIntent = Intent(applicationContext, TorrentActivity::class.java).apply {
            putExtra(TorrentActivity.Extras.TORRENT_HASH, torrent.hash)
            putExtra(TorrentActivity.Extras.SERVER_ID, serverId)

            action = torrent.hash
        }

        val torrentPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            torrentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(applicationContext, "channel_server_${serverId}_downloaded")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(applicationContext.getString(R.string.notification_torrent_downloaded, torrent.name))
            .setSubText(serverConfig.name ?: serverConfig.visibleUrl)
            .setGroup("torrent_downloaded_$serverId")
            .setSortKey(torrent.name.lowercase())
            .addAction(
                R.drawable.ic_notification,
                applicationContext.getString(R.string.notification_view_torrent),
                torrentPendingIntent
            )
            .build()

        notificationManager.notify("torrent_downloaded_${serverId}_${torrent.hash}", 0, notification)
    }

    private fun areNotificationsEnabled() = NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()

    private fun isNotificationChannelEnabled(name: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.getNotificationChannel(name).importance != NotificationManager.IMPORTANCE_NONE
    } else {
        true
    }
}
