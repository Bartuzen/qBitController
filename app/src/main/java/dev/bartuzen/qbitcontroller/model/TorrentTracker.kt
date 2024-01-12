package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.NullableIntDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.NullableStringDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.TrackerTierDeserializer

data class TorrentTracker(
    @JsonProperty("url")
    val url: String,

    @JsonProperty("tier")
    @JsonDeserialize(using = TrackerTierDeserializer::class)
    val tier: Int?,

    @JsonProperty("num_peers")
    @JsonDeserialize(using = NullableIntDeserializer::class)
    val peers: Int?,

    @JsonProperty("num_seeds")
    @JsonDeserialize(using = NullableIntDeserializer::class)
    val seeds: Int?,

    @JsonProperty("num_leeches")
    @JsonDeserialize(using = NullableIntDeserializer::class)
    val leeches: Int?,

    @JsonProperty("num_downloaded")
    @JsonDeserialize(using = NullableIntDeserializer::class)
    val downloaded: Int?,

    @JsonProperty("msg")
    @JsonDeserialize(using = NullableStringDeserializer::class)
    val message: String?
)
