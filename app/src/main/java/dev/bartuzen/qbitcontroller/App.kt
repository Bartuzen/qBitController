package dev.bartuzen.qbitcontroller

import android.app.Application
import android.app.UiModeManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import dev.bartuzen.qbitcontroller.data.ConfigMigrator
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.data.notification.AppNotificationManager
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
    @Suppress("ktlint:standard:backing-property-naming")
    lateinit var _settingsManager: Lazy<SettingsManager>
    private val settingsManager: SettingsManager get() = _settingsManager.get()

    @Inject
    lateinit var configMigrator: ConfigMigrator

    @Inject
    @Suppress("ktlint:standard:backing-property-naming")
    lateinit var _notificationManager: Lazy<AppNotificationManager>
    private val notificationManager: AppNotificationManager get() = _notificationManager.get()

    override val workManagerConfiguration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        configMigrator.run()

        notificationManager.updateChannels()
        CoroutineScope(Dispatchers.Main).launch {
            settingsManager.notificationCheckInterval.flow.collectLatest {
                notificationManager.startWorker()
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            settingsManager.theme.flow.collectLatest { theme ->
                val uiModeManager = getSystemService<UiModeManager>()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && uiModeManager != null) {
                    val mode = when (theme) {
                        Theme.LIGHT -> UiModeManager.MODE_NIGHT_NO
                        Theme.DARK -> UiModeManager.MODE_NIGHT_YES
                        Theme.SYSTEM_DEFAULT -> UiModeManager.MODE_NIGHT_CUSTOM
                    }
                    uiModeManager.setApplicationNightMode(mode)
                } else {
                    val mode = when (theme) {
                        Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                        Theme.SYSTEM_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
        }
    }
}
