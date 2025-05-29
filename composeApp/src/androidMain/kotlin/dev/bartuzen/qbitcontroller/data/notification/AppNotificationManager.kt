package dev.bartuzen.qbitcontroller.data.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.utils.getString
import kotlinx.coroutines.runBlocking
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.notification_channel_download_completed
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class AppNotificationManager(
    private val serverManager: ServerManager,
    private val settingsManager: SettingsManager,
    private val context: Context,
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        serverManager.addServerListener(
            add = { updateChannels() },
            remove = { updateChannels() },
            change = { updateChannels() },
        )
    }

    fun startWorker() {
        val workManager = WorkManager.getInstance(context)

        val repeatInterval = settingsManager.notificationCheckInterval.value.toLong()
        val areNotificationsEnabled =
            NotificationManagerCompat.from(context).areNotificationsEnabled() && repeatInterval != 0L
        if (!areNotificationsEnabled) {
            workManager.cancelUniqueWork("torrent_downloaded")
            return
        }

        val currentWork = workManager.getWorkInfosForUniqueWork("torrent_downloaded").get().firstOrNull()

        if (currentWork != null &&
            currentWork.state != WorkInfo.State.CANCELLED &&
            currentWork.periodicityInfo?.repeatIntervalMillis?.milliseconds == repeatInterval.minutes
        ) {
            return
        }

        val work = PeriodicWorkRequestBuilder<TorrentDownloadedWorker>(repeatInterval, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).build()

        workManager.enqueueUniquePeriodicWork("torrent_downloaded", ExistingPeriodicWorkPolicy.REPLACE, work)
    }

    fun updateChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        createChannels()
        removeUnusedChannels()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannels() {
        serverManager.serversFlow.value.forEach { serverConfig ->
            val notificationGroup =
                NotificationChannelGroup("group_server_${serverConfig.id}", serverConfig.name ?: serverConfig.visibleUrl)

            val downloadedChannel = NotificationChannel(
                "channel_server_${serverConfig.id}_downloaded",
                runBlocking { getString(Res.string.notification_channel_download_completed) },
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                group = "group_server_${serverConfig.id}"
            }

            notificationManager.createNotificationChannelGroup(notificationGroup)
            notificationManager.createNotificationChannel(downloadedChannel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun removeUnusedChannels() {
        notificationManager.notificationChannelGroups
            .filter { it.id.startsWith("group_server_") }
            .forEach { group ->
                val serverId = group.id.replace("group_server_", "").toInt()

                if (serverManager.getServerOrNull(serverId) == null) {
                    notificationManager.deleteNotificationChannelGroup(group.id)
                }
            }
    }
}
