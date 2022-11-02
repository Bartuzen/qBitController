package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class TorrentFile(
    @JsonProperty("index")
    val index: Int,

    @JsonProperty("name")
    val name: String
)
