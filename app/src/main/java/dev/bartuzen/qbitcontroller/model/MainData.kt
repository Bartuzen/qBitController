package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.MainDataDeserializer

@JsonDeserialize(using = MainDataDeserializer::class)
data class MainData(
    val serverState: ServerState,
    val torrents: List<Torrent>,
    val categories: List<Category>,
    val tags: List<String>
)
