package dev.bartuzen.qbitcontroller.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.fasterxml.jackson.annotation.JsonProperty
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
}

typealias ServerConfigMap = SortedMap<Int, ServerConfig>

enum class Theme {
    LIGHT, DARK, SYSTEM_DEFAULT
}

enum class TorrentSort {
    @JsonProperty("name")
    NAME,

    @JsonProperty("hash")
    HASH,

    @JsonProperty("dlspeed")
    DOWNLOAD_SPEED,

    @JsonProperty("upspeed")
    UPLOAD_SPEED,

    @JsonProperty("priority")
    PRIORITY
}

fun Theme.toDelegate() = when (this) {
    Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    Theme.SYSTEM_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}
