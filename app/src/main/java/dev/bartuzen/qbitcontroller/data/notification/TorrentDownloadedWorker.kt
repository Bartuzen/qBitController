package dev.bartuzen.qbitcontroller.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import dev.bartuzen.qbitcontroller.ui.main.MainActivity
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

    private val mapper = jacksonObjectMapper()

    private val sharedPref = appContext.getSharedPreferences("torrents", Context.MODE_PRIVATE)

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
            clearSharedPref()
            return
        }

        val servers = serverManager.serversFlow.value

        getSavedServers().forEach { serverId ->
            if (serverId !in servers) {
                removeServerFromSharedPref(serverId)
            }
        }

        servers.forEach { (serverId, serverConfig) ->
            if (!isNotificationChannelEnabled("channel_server_${serverId}_downloaded")) {
                removeServerFromSharedPref(serverId)
                return@forEach
            }

            val result = requestManager.request(serverId) { service ->
                service.getTorrentList()
            }

            if (result is RequestResult.Success) {
                val oldTorrents = getTorrents(serverId)
                val newTorrents = result.data

                setTorrents(serverId, newTorrents.associate { it.hash to it.state })

                if (oldTorrents == null) {
                    return@forEach
                }

                newTorrents.forEach { torrent ->
                    val newState = oldTorrents[torrent.hash]

                    if (torrent.state in completedStates && (newState == null || newState in downloadingStates)) {
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
            putExtra(TorrentActivity.Extras.DISMISS_NOTIFICATION, true)

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
            .setContentTitle(torrent.name)
            .setContentText(applicationContext.getString(R.string.notification_torrent_downloaded))
            .setGroup("torrent_downloaded_$serverId")
            .setSortKey(torrent.name.lowercase())
            .setContentIntent(torrentPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify("torrent_downloaded_${serverId}_${torrent.hash}", 0, notification)

        sendSummaryNotification(serverConfig)
    }

    private fun sendSummaryNotification(serverConfig: ServerConfig) {
        val serverId = serverConfig.id

        val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val mainPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            mainIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val summaryNotification = NotificationCompat.Builder(applicationContext, "channel_server_${serverId}_downloaded")
            .setSmallIcon(R.drawable.ic_notification)
            .setSubText(serverConfig.name ?: serverConfig.visibleUrl)
            .setGroup("torrent_downloaded_$serverId")
            .setGroupSummary(true)
            .setContentIntent(mainPendingIntent)
            .build()

        notificationManager.notify("torrent_downloaded_summary_$serverId", 0, summaryNotification)
    }

    private fun areNotificationsEnabled() = NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()

    private fun isNotificationChannelEnabled(name: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.getNotificationChannel(name).importance != NotificationManager.IMPORTANCE_NONE
    } else {
        true
    }

    private fun getTorrents(serverId: Int): Map<String, TorrentState>? {
        val json = sharedPref.getString("server_$serverId", null)
        return if (json != null) mapper.readValue(json) else null
    }

    private fun setTorrents(serverId: Int, torrents: Map<String, TorrentState>) {
        val json = mapper.writeValueAsString(torrents)
        sharedPref.edit {
            putString("server_$serverId", json)
        }
    }

    private fun getSavedServers() = sharedPref.all.map { (key, _) ->
        key.replace("server_", "").toIntOrNull()
    }.filterNotNull()

    private fun clearSharedPref() {
        sharedPref.edit {
            clear()
        }
    }

    private fun removeServerFromSharedPref(serverId: Int) {
        sharedPref.edit {
            remove("server_$serverId")
        }
    }
}
