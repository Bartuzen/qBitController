package dev.bartuzen.qbitcontroller.data.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration.Companion.minutes

@HiltWorker
class TorrentDownloadedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workParams: WorkerParameters,
    private val requestManager: RequestManager,
    private val serverManager: ServerManager,
    private val settingsManager: SettingsManager,
    private val notifier: TorrentDownloadedNotifier,
) : CoroutineWorker(appContext, workParams) {
    private val notificationManager = NotificationManagerCompat.from(applicationContext)

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
        notifier.discardRemovedServers()

        if (!notificationManager.areNotificationsEnabled()) {
            WorkManager.getInstance(applicationContext)
                .cancelUniqueWork("torrent_downloaded")
            return
        }

        val serverIds = serverManager.serversFlow.value.keys
            .filter { isDownloadNotificationsEnabled(it) }

        supervisorScope {
            val semaphore = Semaphore(5)
            serverIds.forEach { serverId ->
                launch {
                    val result = semaphore.withPermit {
                        requestManager.request(serverId) { service ->
                            service.getTorrentList()
                        }
                    }

                    if (result is RequestResult.Success) {
                        notifier.checkCompleted(serverId, result.data)
                    }
                }
            }
        }
    }

    private fun isDownloadNotificationsEnabled(serverId: Int) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = notificationManager.getNotificationChannel("channel_server_${serverId}_downloaded")
        channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
    } else {
        true
    }
}
