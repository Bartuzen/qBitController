package dev.bartuzen.qbitcontroller.model

import dev.bartuzen.qbitcontroller.model.serializers.NullableIntSerializer
import dev.bartuzen.qbitcontroller.model.serializers.NullableStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorrentTracker(
    @SerialName("url")
    val url: String,

    @SerialName("tier")
    val tier: Int,

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
    val message: String?
)
