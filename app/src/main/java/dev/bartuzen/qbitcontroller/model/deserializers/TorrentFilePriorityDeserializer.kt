package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import dev.bartuzen.qbitcontroller.model.TorrentFilePriority

class TorrentFilePriorityDeserializer : JsonDeserializer<TorrentFilePriority>() {
    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): TorrentFilePriority {
        return when (val priority = parser?.valueAsInt) {
            0 -> TorrentFilePriority.DO_NOT_DOWNLOAD
            1 -> TorrentFilePriority.NORMAL
            6 -> TorrentFilePriority.HIGH
            7 -> TorrentFilePriority.MAXIMUM
            else -> throw IllegalArgumentException("Unknown priority $priority")
        }
    }
}
