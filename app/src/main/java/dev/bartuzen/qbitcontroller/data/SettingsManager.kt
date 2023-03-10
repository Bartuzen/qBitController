package dev.bartuzen.qbitcontroller.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.sharedpreferences.enumPreference
import dev.bartuzen.qbitcontroller.utils.sharedpreferences.primitivePreference
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val theme = enumPreference(sharedPref, "theme", Theme.SYSTEM_DEFAULT, Theme::valueOf)
    val sort = enumPreference(sharedPref, "sort", TorrentSort.NAME, TorrentSort::valueOf)
    val isReverseSorting = primitivePreference(sharedPref, "isReverseSorting", false)
    val connectionTimeout = primitivePreference(sharedPref, "connectionTimeout", 10)
    val autoRefreshInterval = primitivePreference(sharedPref, "autoRefreshInterval", 0)
    val autoRefreshHideLoadingBar = primitivePreference(sharedPref, "autoRefreshHideLoadingBar", false)
    val notificationCheckInterval = primitivePreference(sharedPref, "notificationCheckInterval", 15)
}

typealias ServerConfigMap = SortedMap<Int, ServerConfig>

enum class Theme {
    LIGHT, DARK, SYSTEM_DEFAULT
}

enum class TorrentSort {
    NAME,
    STATUS,
    HASH,
    DOWNLOAD_SPEED,
    UPLOAD_SPEED,
    PRIORITY,
    ETA,
    SIZE,
    PROGRESS,
    CONNECTED_SEEDS,
    TOTAL_SEEDS,
    CONNECTED_LEECHES,
    TOTAL_LEECHES,
    ADDITION_DATE,
    COMPLETION_DATE
}

fun Theme.toDelegate() = when (this) {
    Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    Theme.SYSTEM_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}
