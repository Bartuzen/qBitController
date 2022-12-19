package dev.bartuzen.qbitcontroller

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import dev.bartuzen.qbitcontroller.data.ConfigMigrator
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.toDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var _settingsManager: Lazy<SettingsManager>
    private val settingsManager: SettingsManager get() = _settingsManager.get()

    @Inject
    lateinit var configMigrator: ConfigMigrator

    override fun onCreate() {
        super.onCreate()

        configMigrator.run()

        CoroutineScope(Dispatchers.Main).launch {
            settingsManager.theme.flow.collectLatest { theme ->
                AppCompatDelegate.setDefaultNightMode(theme.toDelegate())
            }
        }
    }
}
