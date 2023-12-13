package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.Serializable

@Serializable
data class TorrentWebSeed(
    val url: String
)
