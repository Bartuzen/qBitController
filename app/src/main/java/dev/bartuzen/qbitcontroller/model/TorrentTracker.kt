package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.TrackerPropertyDeserializer

data class TorrentTracker(
    @JsonProperty("url") val url: String,
    @JsonProperty("num_peers") @JsonDeserialize(using = TrackerPropertyDeserializer::class) val peers: Int?,
    @JsonProperty("num_seeds") @JsonDeserialize(using = TrackerPropertyDeserializer::class) val seeds: Int?,
    @JsonProperty("num_leeches") @JsonDeserialize(using = TrackerPropertyDeserializer::class) val leeches: Int?,
    @JsonProperty("num_downloaded") @JsonDeserialize(using = TrackerPropertyDeserializer::class) val downloaded: Int?
)