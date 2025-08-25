package dev.bartuzen.qbitcontroller

import android.app.Application
import android.app.UiModeManager
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import com.google.android.material.color.DynamicColors
import dev.bartuzen.qbitcontroller.data.ConfigMigrator
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.data.notification.AppNotificationManager
import dev.bartuzen.qbitcontroller.di.appModule
import dev.bartuzen.qbitcontroller.utils.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.shortcut_new_window

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            workManagerFactory()
            androidLogger()
            modules(appModule)
        }

        DynamicColors.applyToActivitiesIfAvailable(this)

        val configMigrator by inject<ConfigMigrator>()
        configMigrator.run()

        val settingsManager by inject<SettingsManager>()
        val notificationManager by inject<AppNotificationManager>()

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

        registerShortcuts()
    }

    private fun registerShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService<ShortcutManager>() ?: return

            val newWindowShortcut = ShortcutInfo.Builder(this, "new_window")
                .setShortLabel(runBlocking { getString(Res.string.shortcut_new_window) })
                .setIcon(Icon.createWithResource(this, R.drawable.sc_new_window))
                .setIntent(
                    Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    },
                )
                .build()

            shortcutManager.dynamicShortcuts = listOf(newWindowShortcut)
        }
    }
}
