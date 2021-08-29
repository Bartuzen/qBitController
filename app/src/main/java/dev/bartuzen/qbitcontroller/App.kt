package dev.bartuzen.qbitcontroller

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.asLiveData
import dagger.hilt.android.HiltAndroidApp
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.toDelegate
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        settingsManager.themeFlow.asLiveData().observeForever { theme ->
            AppCompatDelegate.setDefaultNightMode(theme.toDelegate())
        }
    }
}