package dev.bartuzen.qbitcontroller.ui.main

sealed class DeepLinkDestination {
    data class TorrentList(val serverId: Int?) : DeepLinkDestination()
    data class Torrent(val serverId: Int, val torrentHash: String, val torrentName: String?) : DeepLinkDestination()
    data class AddTorrent(val torrentUrl: String?, val torrentFileUris: List<String>?) : DeepLinkDestination()
    data object Settings : DeepLinkDestination()
}
