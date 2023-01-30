package dev.bartuzen.qbitcontroller.model

data class MainData(
    val torrents: List<Torrent>,
    val categories: List<Category>,
    val tags: List<String>
)
