package dev.bartuzen.qbitcontroller.model

import dev.bartuzen.qbitcontroller.model.serializers.NullableIntSerializer
import dev.bartuzen.qbitcontroller.model.serializers.NullableStringSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class TorrentTracker(
    @SerialName("url")
    val url: String,

    @SerialName("tier")
    @Serializable(with = TrackerTierSerializer::class)
    val tier: Int?,

    @SerialName("num_peers")
    @Serializable(with = NullableIntSerializer::class)
    val peers: Int?,

    @SerialName("num_seeds")
    @Serializable(with = NullableIntSerializer::class)
    val seeds: Int?,

    @SerialName("num_leeches")
    @Serializable(with = NullableIntSerializer::class)
    val leeches: Int?,

    @SerialName("num_downloaded")
    @Serializable(with = NullableIntSerializer::class)
    val downloaded: Int?,

    @SerialName("msg")
    @Serializable(with = NullableStringSerializer::class)
    val message: String?,
)

private object TrackerTierSerializer : KSerializer<Int?> {
    override val descriptor = JsonPrimitive.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Int?) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): Int? {
        val jsonValue = (decoder as JsonDecoder).decodeJsonElement().jsonPrimitive
        // Older versions return empty string instead of -1 for default trackers
        return jsonValue.intOrNull.takeIf { it != -1 }
    }
}
