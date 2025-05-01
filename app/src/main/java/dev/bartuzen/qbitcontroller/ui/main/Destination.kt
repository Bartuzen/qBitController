package dev.bartuzen.qbitcontroller.ui.main

import kotlinx.serialization.Serializable

sealed class Destination {
    @Serializable
    data object TorrentList : Destination()

    @Serializable
    data class Torrent(val serverId: Int, val torrentHash: String, val torrentName: String?) : Destination()

    @Serializable
    data class AddTorrent(
        val initialServerId: Int? = null,
        val torrentUrl: String? = null,
        val torrentFileUris: List<String>? = null,
    ) : Destination()

    sealed class Rss {
        @Serializable
        data class Feeds(val serverId: Int) : Destination()

        @Serializable
        data class Articles(val serverId: Int, val feedPath: List<String>, val uid: String?) : Destination()

        @Serializable
        data class Rules(val serverId: Int) : Destination()

        @Serializable
        data class EditRule(val serverId: Int, val ruleName: String) : Destination()
    }

    sealed class Search {
        @Serializable
        data class Start(val serverId: Int) : Destination()

        @Serializable
        data class Result(val serverId: Int, val searchQuery: String, val category: String, val plugins: String) :
            Destination()

        @Serializable
        data class Plugins(val serverId: Int) : Destination()
    }

    @Serializable
    data class Log(val serverId: Int) : Destination()
}
