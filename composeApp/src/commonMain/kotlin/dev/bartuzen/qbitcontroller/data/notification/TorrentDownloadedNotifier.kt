package dev.bartuzen.qbitcontroller.data.notification

import dev.bartuzen.qbitcontroller.model.Torrent

expect class TorrentDownloadedNotifier() {
    fun checkCompleted(serverId: Int, torrentList: List<Torrent>)
    fun checkCompleted(serverId: Int, torrent: Torrent)
}
