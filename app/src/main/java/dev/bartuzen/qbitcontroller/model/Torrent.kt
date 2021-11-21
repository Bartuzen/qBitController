package dev.bartuzen.qbitcontroller.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.Parcelize

@JsonIgnoreProperties(ignoreUnknown = true)
@Parcelize
data class Torrent(
    @JsonProperty("hash") val hash: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("state") val state: TorrentState?,
    @JsonProperty("downloaded") val downloaded: Long,
    @JsonProperty("size") val size: Long,
    @JsonProperty("eta") val eta: Long,
    @JsonProperty("dlspeed") val downloadSpeed: Long,
    @JsonProperty("upspeed") val uploadSpeed: Long,
    @JsonProperty("progress") val progress: Double,
    @JsonProperty("completed") val completed: Long,
) : Parcelable

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