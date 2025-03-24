package dev.bartuzen.qbitcontroller.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentFilter
import dev.bartuzen.qbitcontroller.utils.sharedpreferences.enumPreference
import dev.bartuzen.qbitcontroller.utils.sharedpreferences.primitivePreference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val theme = enumPreference(sharedPref, "theme", Theme.SYSTEM_DEFAULT, Theme::valueOf)
    val sort = enumPreference(sharedPref, "sort", TorrentSort.NAME, TorrentSort::valueOf)
    val isReverseSorting = primitivePreference(sharedPref, "isReverseSorting", false)
    val connectionTimeout = primitivePreference(sharedPref, "connectionTimeout", 10)
    val autoRefreshInterval = primitivePreference(sharedPref, "autoRefreshInterval", 0)
    val notificationCheckInterval = primitivePreference(sharedPref, "notificationCheckInterval", 15)
    val areTorrentSwipeActionsEnabled = primitivePreference(sharedPref, "areTorrentSwipeActionsEnabled", true)

    val defaultTorrentStatus = enumPreference(sharedPref, "defaultTorrentState", TorrentFilter.ALL, TorrentFilter::valueOf)
    val areStatesCollapsed = primitivePreference(sharedPref, "areStatesCollapsed", false)
    val areCategoriesCollapsed = primitivePreference(sharedPref, "areCategoriesCollapsed", false)
    val areTagsCollapsed = primitivePreference(sharedPref, "areTagsCollapsed", false)
    val areTrackersCollapsed = primitivePreference(sharedPref, "areTrackersCollapsed", false)

    val searchSort = enumPreference(sharedPref, "searchSort", SearchSort.NAME, SearchSort::valueOf)
    val isReverseSearchSorting = primitivePreference(sharedPref, "isReverseSearchSort", false)
}

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM_DEFAULT,
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
    RATIO,
    PROGRESS,
    CONNECTED_SEEDS,
    TOTAL_SEEDS,
    CONNECTED_LEECHES,
    TOTAL_LEECHES,
    ADDITION_DATE,
    COMPLETION_DATE,
    LAST_ACTIVITY,
}

enum class SearchSort {
    NAME,
    SIZE,
    SEEDERS,
    LEECHERS,
    SEARCH_ENGINE,
}
