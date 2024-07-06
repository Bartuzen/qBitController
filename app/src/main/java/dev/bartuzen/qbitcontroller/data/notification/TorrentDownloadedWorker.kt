package dev.bartuzen.qbitcontroller.data.notification

import android.content.Context
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

        val areNotificationsEnabled = NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        if (!areNotificationsEnabled) {
            WorkManager.getInstance(applicationContext)
                .cancelUniqueWork("torrent_downloaded")
            return
        }

        serverManager.serversFlow.value.keys.forEach { serverId ->
            val result = requestManager.request(serverId) { service ->
                service.getTorrentList()
            }

            if (result is RequestResult.Success) {
                notifier.checkCompleted(serverId, result.data)
            }
        }
    }
}
