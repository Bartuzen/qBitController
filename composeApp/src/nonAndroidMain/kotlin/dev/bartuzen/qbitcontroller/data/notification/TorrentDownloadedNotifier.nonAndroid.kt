package dev.bartuzen.qbitcontroller.data.notification

import dev.bartuzen.qbitcontroller.model.Torrent

actual class TorrentDownloadedNotifier {
    actual fun checkCompleted(serverId: Int, torrentList: List<Torrent>) {}
    actual fun checkCompleted(serverId: Int, torrent: Torrent) {}
}
