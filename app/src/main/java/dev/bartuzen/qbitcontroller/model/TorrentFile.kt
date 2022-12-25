package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.TorrentFilePriorityDeserializer

data class TorrentFile(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("progress")
    val progress: Double,

    @JsonProperty("priority")
    @JsonDeserialize(using = TorrentFilePriorityDeserializer::class)
    val priority: TorrentFilePriority
)

enum class TorrentFilePriority {
    DO_NOT_DOWNLOAD, NORMAL, HIGH, MAXIMUM
}
