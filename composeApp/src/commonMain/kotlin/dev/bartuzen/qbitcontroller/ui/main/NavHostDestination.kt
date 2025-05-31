package dev.bartuzen.qbitcontroller.ui.main

import kotlinx.serialization.Serializable

sealed class NavHostDestination {
    @Serializable
    data object Torrents : NavHostDestination()

    @Serializable
    data object Search : NavHostDestination()

    @Serializable
    data object Rss : NavHostDestination()

    @Serializable
    data object Logs : NavHostDestination()

    @Serializable
    data object Settings : NavHostDestination()
}
