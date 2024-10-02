package dev.bartuzen.qbitcontroller.model

import java.time.Instant

enum class QBittorrentVersion {
    V4,
    V5,
}

data class QBittorrentVersionCache(
    val fetchDate: Instant,
    val version: QBittorrentVersion,
)
