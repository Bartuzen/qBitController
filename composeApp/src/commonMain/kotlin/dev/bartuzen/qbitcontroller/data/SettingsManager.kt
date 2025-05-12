package dev.bartuzen.qbitcontroller.data

import com.russhwolf.settings.Settings
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentFilter

class SettingsManager(
    settings: Settings,
) {
    val theme = preference(settings, "theme", Theme.SYSTEM_DEFAULT)
    val sort = preference(settings, "sort", TorrentSort.NAME)
    val isReverseSorting = preference(settings, "isReverseSorting", false)
    val connectionTimeout = preference(settings, "connectionTimeout", 10)
    val autoRefreshInterval = preference(settings, "autoRefreshInterval", 0)
    val notificationCheckInterval = preference(settings, "notificationCheckInterval", 15)
    val areTorrentSwipeActionsEnabled = preference(settings, "areTorrentSwipeActionsEnabled", true)

    val defaultTorrentStatus = preference(settings, "defaultTorrentState", TorrentFilter.ALL)
    val areStatesCollapsed = preference(settings, "areStatesCollapsed", false)
    val areCategoriesCollapsed = preference(settings, "areCategoriesCollapsed", false)
    val areTagsCollapsed = preference(settings, "areTagsCollapsed", false)
    val areTrackersCollapsed = preference(settings, "areTrackersCollapsed", false)

    val searchSort = preference(settings, "searchSort", SearchSort.NAME)
    val isReverseSearchSorting = preference(settings, "isReverseSearchSort", false)
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
