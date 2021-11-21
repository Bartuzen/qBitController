package dev.bartuzen.qbitcontroller

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.toDelegate
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        MainScope().launch {
            settingsManager.themeFlow.collectLatest { theme ->
                AppCompatDelegate.setDefaultNightMode(theme.toDelegate())
            }
        }
    }
}