package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorrentPeers(
    @SerialName("peers")
    val peers: Map<String, TorrentPeer>,
)
