package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.NullableStringDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.TagDeserializer

data class Torrent(
    @JsonProperty("hash")
    val hash: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("state")
    val state: TorrentState?,

    @JsonProperty("completed")
    val completed: Long,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("eta")
    val eta: Int,

    @JsonProperty("dlspeed")
    val downloadSpeed: Long,

    @JsonProperty("upspeed")
    val uploadSpeed: Long,

    @JsonProperty("progress")
    val progress: Double,

    @JsonProperty("category")
    @JsonDeserialize(using = NullableStringDeserializer::class)
    val category: String?,

    @JsonProperty("tags")
    @JsonDeserialize(using = TagDeserializer::class)
    val tags: List<String>

)

@Suppress("unused")
enum class TorrentState {
    @JsonProperty("error")
    ERROR,

    @JsonProperty("missingFiles")
    MISSING_FILES,

    @JsonProperty("uploading")
    UPLOADING,

    @JsonProperty("pausedUP")
    PAUSED_UP,

    @JsonProperty("queuedUP")
    QUEUED_UP,

    @JsonProperty("stalledUP")
    STALLED_UP,

    @JsonProperty("checkingUP")
    CHECKING_UP,

    @JsonProperty("forcedUP")
    FORCED_UP,

    @JsonProperty("allocating")
    ALLOCATING,

    @JsonProperty("downloading")
    DOWNLOADING,

    @JsonProperty("metaDL")
    META_DL,

    @JsonProperty("pausedDL")
    PAUSED_DL,

    @JsonProperty("queuedDL")
    QUEUED_DL,

    @JsonProperty("stalledDL")
    STALLED_DL,

    @JsonProperty("checkingDL")
    CHECKING_DL,

    @JsonProperty("forcedDL")
    FORCED_DL,

    @JsonProperty("checkingResumeData")
    CHECKING_RESUME_DATA,

    @JsonProperty("moving")
    MOVING,

    @JsonProperty("unknown")
    UNKNOWN
}

@Suppress("unused")
enum class PieceState {
    NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED
}