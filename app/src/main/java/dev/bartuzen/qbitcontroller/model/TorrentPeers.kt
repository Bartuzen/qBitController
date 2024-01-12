package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class TorrentPeers(
    @JsonProperty("peers")
    val peers: Map<String, TorrentPeer>
)
