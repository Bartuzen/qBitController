package dev.bartuzen.qbitcontroller

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import dev.bartuzen.qbitcontroller.data.ConfigMigrator
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.notification.AppNotificationManager
import dev.bartuzen.qbitcontroller.data.toDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    @Suppress("ktlint:experimental:property-naming")
    lateinit var _settingsManager: Lazy<SettingsManager>
    private val settingsManager: SettingsManager get() = _settingsManager.get()

    @Inject
    lateinit var configMigrator: ConfigMigrator

    @Inject
    lateinit var notificationManager: AppNotificationManager

    override fun getWorkManagerConfiguration() = Configuration.Builder()
        .setWorkerFactory(workerFactory)
        .build()

    override fun onCreate() {
        super.onCreate()

        configMigrator.run()

        notificationManager.updateChannels()
        notificationManager.startWorker()

        CoroutineScope(Dispatchers.Main).launch {
            settingsManager.theme.flow.collectLatest { theme ->
                AppCompatDelegate.setDefaultNightMode(theme.toDelegate())
            }
        }
    }
}
