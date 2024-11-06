package dev.bartuzen.qbitcontroller.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.ui.main.MainActivity
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentDownloadedNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverManager: ServerManager,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val sharedPref = context.getSharedPreferences("torrents", Context.MODE_PRIVATE)

    private val completedStates = listOf(
        TorrentState.UPLOADING,
        TorrentState.STALLED_UP,
        TorrentState.PAUSED_UP,
        TorrentState.FORCED_UP,
        TorrentState.QUEUED_UP,
        TorrentState.CHECKING_UP,
    )

    private val downloadingStates = listOf(
        TorrentState.DOWNLOADING,
        TorrentState.CHECKING_DL,
        TorrentState.FORCED_META_DL,
        TorrentState.QUEUED_DL,
        TorrentState.META_DL,
        TorrentState.FORCED_DL,
        TorrentState.PAUSED_DL,
        TorrentState.STALLED_DL,
    )

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun checkCompleted(serverId: Int, torrentList: List<Torrent>) {
        if (!areNotificationsEnabled()) {
            clearSharedPref()
            return
        }

        if (!isNotificationChannelEnabled("channel_server_${serverId}_downloaded")) {
            removeServerFromSharedPref(serverId)
            return
        }

        val oldTorrents = getTorrents(serverId)

        setTorrents(serverId, torrentList.associate { it.hash to it.state })

        if (oldTorrents == null) {
            return
        }

        torrentList.forEach { torrent ->
            val oldState = oldTorrents[torrent.hash]

            if (torrent.state in completedStates && (oldState == null || oldState in downloadingStates)) {
                sendNotification(serverId, torrent)
            }
        }
    }

    fun checkCompleted(serverId: Int, torrent: Torrent) {
        if (!areNotificationsEnabled()) {
            clearSharedPref()
            return
        }

        if (!isNotificationChannelEnabled("channel_server_${serverId}_downloaded")) {
            removeServerFromSharedPref(serverId)
            return
        }

        val oldTorrents = getTorrents(serverId)

        setTorrent(serverId, torrent)

        if (oldTorrents == null) {
            return
        }

        val oldState = oldTorrents[torrent.hash]
        if (torrent.state in completedStates && (oldState == null || oldState in downloadingStates)) {
            sendNotification(serverId, torrent)
        }
    }

    private fun sendNotification(serverId: Int, torrent: Torrent) {
        val torrentIntent = Intent(context, TorrentActivity::class.java).apply {
            putExtra(TorrentActivity.Extras.TORRENT_HASH, torrent.hash)
            putExtra(TorrentActivity.Extras.SERVER_ID, serverId)

            action = torrent.hash
        }

        val torrentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            torrentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0,
        )

        val notification = NotificationCompat.Builder(context, "channel_server_${serverId}_downloaded")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_torrent_downloaded))
            .setContentText(torrent.name)
            .setGroup("torrent_downloaded_$serverId")
            .setSortKey(torrent.name.lowercase())
            .setContentIntent(torrentPendingIntent)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setAutoCancel(true)
            .build()

        notificationManager.notify("torrent_downloaded_${serverId}_${torrent.hash}", 0, notification)

        sendSummaryNotification(serverId)
    }

    private fun sendSummaryNotification(serverId: Int) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.Extras.SERVER_ID, serverId)

            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val mainPendingIntent = PendingIntent.getActivity(
            context,
            serverId,
            mainIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            },
        )

        val serverConfig = serverManager.getServer(serverId)

        val summaryNotification = NotificationCompat.Builder(context, "channel_server_${serverId}_downloaded")
            .setSmallIcon(R.drawable.ic_notification)
            .setSubText(serverConfig.name ?: serverConfig.visibleUrl)
            .setGroup("torrent_downloaded_$serverId")
            .setGroupSummary(true)
            .setContentIntent(mainPendingIntent)
            .build()

        notificationManager.notify("torrent_downloaded_summary_$serverId", 0, summaryNotification)
    }

    private fun areNotificationsEnabled() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun isNotificationChannelEnabled(name: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = notificationManager.getNotificationChannel(name)
        if (channel != null) {
            channel.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            false
        }
    } else {
        true
    }

    private fun getTorrents(serverId: Int): Map<String, TorrentState>? {
        val json = sharedPref.getString("server_$serverId", null)
        return if (json != null) this.json.decodeFromString(json) else null
    }

    private fun setTorrents(serverId: Int, torrents: Map<String, TorrentState>) {
        val json = json.encodeToString(torrents)
        sharedPref.edit {
            putString("server_$serverId", json)
        }
    }

    private fun setTorrent(serverId: Int, torrent: Torrent) {
        val torrents = getTorrents(serverId).let { torrents ->
            torrents?.toMutableMap()?.also { mutableTorrent ->
                mutableTorrent[torrent.hash] = torrent.state
            } ?: mapOf(torrent.hash to torrent.state)
        }
        setTorrents(serverId, torrents)
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

    fun discardRemovedServers() {
        val servers = serverManager.serversFlow.value
        getSavedServers().forEach { serverId ->
            if (serverId !in servers) {
                removeServerFromSharedPref(serverId)
            }
        }
    }
}
