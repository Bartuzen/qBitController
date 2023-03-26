package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.EtaDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.NullableEpochTimeDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.NullableStringDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.PriorityDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.TagDeserializer

data class Torrent(
    @JsonProperty("hash")
    val hash: String,

    @JsonProperty("infohash_v1")
    @JsonDeserialize(using = NullableStringDeserializer::class)
    val hashV1: String?,

    @JsonProperty("infohash_v2")
    @JsonDeserialize(using = NullableStringDeserializer::class)
    val hashV2: String?,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("state")
    val state: TorrentState,

    @JsonProperty("added_on")
    val additionDate: Long,

    @JsonProperty("completion_on")
    @JsonDeserialize(using = NullableEpochTimeDeserializer::class)
    val completionDate: Long?,

    @JsonProperty("completed")
    val completed: Long,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("eta")
    @JsonDeserialize(using = EtaDeserializer::class)
    val eta: Int?,

    @JsonProperty("dlspeed")
    val downloadSpeed: Long,

    @JsonProperty("upspeed")
    val uploadSpeed: Long,

    @JsonProperty("dl_limit")
    val downloadSpeedLimit: Int,

    @JsonProperty("up_limit")
    val uploadSpeedLimit: Int,

    @JsonProperty("progress")
    val progress: Double,

    @JsonProperty("priority")
    @JsonDeserialize(using = PriorityDeserializer::class)
    val priority: Int?,

    @JsonProperty("num_seeds")
    val connectedSeeds: Int,

    @JsonProperty("num_leechs")
    val connectedLeeches: Int,

    @JsonProperty("num_complete")
    val totalSeeds: Int,

    @JsonProperty("num_incomplete")
    val totalLeeches: Int,

    @JsonProperty("save_path")
    val savePath: String?,

    @JsonProperty("download_path")
    @JsonDeserialize(using = NullableStringDeserializer::class)
    val downloadPath: String?,

    @JsonProperty("category")
    @JsonDeserialize(using = NullableStringDeserializer::class)
    val category: String?,

    @JsonProperty("tags")
    @JsonDeserialize(using = TagDeserializer::class)
    val tags: List<String>,

    @JsonProperty("seq_dl")
    val isSequentialDownloadEnabled: Boolean,

    @JsonProperty("f_l_piece_prio")
    val isFirstLastPiecesPrioritized: Boolean,

    @JsonProperty("auto_tmm")
    val isAutomaticTorrentManagementEnabled: Boolean,

    @JsonProperty("force_start")
    val isForceStartEnabled: Boolean,

    @JsonProperty("super_seeding")
    val isSuperSeedingEnabled: Boolean,

    @JsonProperty("magnet_uri")
    val magnetUri: String,

    @JsonProperty("time_active")
    val timeActive: Long,

    @JsonProperty("downloaded")
    val downloaded: Long,

    @JsonProperty("downloaded_session")
    val downloadedSession: Long,

    @JsonProperty("uploaded")
    val uploaded: Long,

    @JsonProperty("uploaded_session")
    val uploadedSession: Long,

    @JsonProperty("ratio")
    val ratio: Double,

    @JsonProperty("last_activity")
    val lastActivity: Long,

    @JsonProperty("seen_complete")
    @JsonDeserialize(using = NullableEpochTimeDeserializer::class)
    val lastSeenComplete: Long?,

    @JsonProperty("ratio_limit")
    val ratioLimit: Double,

    @JsonProperty("seeding_time_limit")
    val seedingTimeLimit: Int,

    @JsonProperty("trackers_count")
    val trackerCount: Int
)

enum class TorrentState {
    @JsonProperty("forcedDL")
    FORCED_DL,

    @JsonProperty("downloading")
    DOWNLOADING,

    @JsonProperty("forcedMetaDL")
    FORCED_META_DL,

    @JsonProperty("metaDL")
    META_DL,

    @JsonProperty("allocating")
    ALLOCATING,

    @JsonProperty("stalledDL")
    STALLED_DL,

    @JsonProperty("forcedUP")
    FORCED_UP,

    @JsonProperty("uploading")
    UPLOADING,

    @JsonProperty("stalledUP")
    STALLED_UP,

    @JsonProperty("checkingResumeData")
    CHECKING_RESUME_DATA,

    @JsonProperty("queuedDL")
    QUEUED_DL,

    @JsonProperty("queuedUP")
    QUEUED_UP,

    @JsonProperty("checkingUP")
    CHECKING_UP,

    @JsonProperty("checkingDL")
    CHECKING_DL,

    @JsonProperty("pausedDL")
    PAUSED_DL,

    @JsonProperty("pausedUP")
    PAUSED_UP,

    @JsonProperty("moving")
    MOVING,

    @JsonProperty("missingFiles")
    MISSING_FILES,

    @JsonProperty("error")
    ERROR,

    @JsonProperty("unknown")
    @JsonEnumDefaultValue
    UNKNOWN
}

@Suppress("unused")
enum class PieceState {
    NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED
}
