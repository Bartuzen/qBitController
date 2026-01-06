package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class TorrentFile(
    @SerialName("index")
    val index: Int = 0,

    @SerialName("name")
    val name: String,

    @SerialName("size")
    val size: Long,

    @SerialName("progress")
    val progress: Double,

    @SerialName("priority")
    @Serializable(with = TorrentFilePrioritySerializer::class)
    val priority: TorrentFilePriority,
)

object TorrentFilePrioritySerializer : KSerializer<TorrentFilePriority> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TorrentFilePriority", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: TorrentFilePriority) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): TorrentFilePriority {
        val priorityId = decoder.decodeInt()
        // Seems like older versions of qBittorrent use 4 as normal priority
        if (priorityId == 4) {
            return TorrentFilePriority.NORMAL
        }
        return TorrentFilePriority.entries.find { it.id == priorityId }
            ?: throw IllegalArgumentException("Unknown priority: $priorityId")
    }
}

enum class TorrentFilePriority(val id: Int) {
    DO_NOT_DOWNLOAD(0),
    NORMAL(1),
    HIGH(6),
    MAXIMUM(7),
}
