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
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Singleton
class AppNotificationManager @Inject constructor(
    private val serverManager: ServerManager,
    private val settingsManager: SettingsManager,
    @ApplicationContext private val context: Context,
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        serverManager.addServerListener(object : ServerManager.ServerListener {
            override fun onServerAddedListener(serverConfig: ServerConfig) {
                updateChannels()
            }

            override fun onServerRemovedListener(serverConfig: ServerConfig) {
                updateChannels()
            }

            override fun onServerChangedListener(serverConfig: ServerConfig) {
                updateChannels()
            }
        })
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
        serverManager.serversFlow.value.forEach { (_, serverConfig) ->
            val notificationGroup =
                NotificationChannelGroup("group_server_${serverConfig.id}", serverConfig.name ?: serverConfig.visibleUrl)

            val downloadedChannel = NotificationChannel(
                "channel_server_${serverConfig.id}_downloaded",
                context.getString(R.string.notification_channel_download_completed),
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

                if (serverId !in serverManager.serversFlow.value) {
                    notificationManager.deleteNotificationChannelGroup(group.id)
                }
            }
    }
}
