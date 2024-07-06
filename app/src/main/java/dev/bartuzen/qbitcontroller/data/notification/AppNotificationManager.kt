package dev.bartuzen.qbitcontroller.data.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotificationManager @Inject constructor(
    private val serverManager: ServerManager,
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
        val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!areNotificationsEnabled) {
            return
        }

        val workManager = WorkManager.getInstance(context)

        val work = OneTimeWorkRequestBuilder<TorrentDownloadedWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).build()

        workManager.enqueueUniqueWork("torrent_downloaded", ExistingWorkPolicy.KEEP, work)
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
