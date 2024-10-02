package dev.bartuzen.qbitcontroller.model

import dev.bartuzen.qbitcontroller.model.serializers.NullableEpochTimeSerializer
import dev.bartuzen.qbitcontroller.model.serializers.NullableStringSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Torrent(
    @SerialName("hash")
    val hash: String = "",

    @SerialName("infohash_v1")
    @Serializable(with = NullableStringSerializer::class)
    val hashV1: String?,

    @SerialName("infohash_v2")
    @Serializable(with = NullableStringSerializer::class)
    val hashV2: String?,

    @SerialName("name")
    val name: String,

    @SerialName("state")
    val state: TorrentState = TorrentState.UNKNOWN,

    @SerialName("added_on")
    val additionDate: Long,

    @SerialName("completion_on")
    @Serializable(with = NullableEpochTimeSerializer::class)
    val completionDate: Long?,

    @SerialName("completed")
    val completed: Long,

    @SerialName("size")
    val size: Long,

    @SerialName("eta")
    @Serializable(with = EtaSerializer::class)
    val eta: Int?,

    @SerialName("dlspeed")
    val downloadSpeed: Long,

    @SerialName("upspeed")
    val uploadSpeed: Long,

    @SerialName("dl_limit")
    val downloadSpeedLimit: Int,

    @SerialName("up_limit")
    val uploadSpeedLimit: Int,

    @SerialName("progress")
    val progress: Double,

    @SerialName("priority")
    @Serializable(with = PrioritySerializer::class)
    val priority: Int?,

    @SerialName("num_seeds")
    val connectedSeeds: Int,

    @SerialName("num_leechs")
    val connectedLeeches: Int,

    @SerialName("num_complete")
    val totalSeeds: Int,

    @SerialName("num_incomplete")
    val totalLeeches: Int,

    @SerialName("save_path")
    val savePath: String?,

    @SerialName("download_path")
    @Serializable(with = NullableStringSerializer::class)
    val downloadPath: String?,

    @SerialName("category")
    @Serializable(with = NullableStringSerializer::class)
    val category: String?,

    @SerialName("tags")
    @Serializable(with = TagsSerializer::class)
    val tags: List<String>,

    @SerialName("seq_dl")
    val isSequentialDownloadEnabled: Boolean,

    @SerialName("f_l_piece_prio")
    val isFirstLastPiecesPrioritized: Boolean,

    @SerialName("auto_tmm")
    val isAutomaticTorrentManagementEnabled: Boolean,

    @SerialName("force_start")
    val isForceStartEnabled: Boolean,

    @SerialName("super_seeding")
    val isSuperSeedingEnabled: Boolean,

    @SerialName("magnet_uri")
    val magnetUri: String,

    @SerialName("time_active")
    val timeActive: Long,

    @SerialName("downloaded")
    val downloaded: Long,

    @SerialName("downloaded_session")
    val downloadedSession: Long,

    @SerialName("uploaded")
    val uploaded: Long,

    @SerialName("uploaded_session")
    val uploadedSession: Long,

    @SerialName("ratio")
    val ratio: Double,

    @SerialName("last_activity")
    val lastActivity: Long,

    @SerialName("seen_complete")
    @Serializable(with = NullableEpochTimeSerializer::class)
    val lastSeenComplete: Long?,

    @SerialName("ratio_limit")
    val ratioLimit: Double,

    @SerialName("seeding_time_limit")
    val seedingTimeLimit: Int,

    @SerialName("inactive_seeding_time_limit")
    val inactiveSeedingTimeLimit: Int = -1,

    @SerialName("seeding_time")
    val seedingTime: Int = 0,

    @SerialName("trackers_count")
    val trackerCount: Int = 0,

    @SerialName("private")
    val isPrivate: Boolean?,
)

private object EtaSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Eta", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int?) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): Int? {
        val jsonValue = (decoder as JsonDecoder).decodeJsonElement().jsonPrimitive
        return jsonValue.intOrNull.takeIf { it in 0..<8640000 }
    }
}

private object PrioritySerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Priority", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int?) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): Int? {
        return decoder.decodeInt().takeIf { it != 0 }
    }
}

private object TagsSerializer : KSerializer<List<String>> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = listSerialDescriptor<List<String>>()

    override fun serialize(encoder: Encoder, value: List<String>) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): List<String> {
        return decoder.decodeString().takeIf { it.isNotEmpty() }?.split(", ") ?: emptyList()
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = TorrentStateSerializer::class)
@KeepGeneratedSerializer
enum class TorrentState {
    @SerialName("forcedDL")
    FORCED_DL,

    @SerialName("downloading")
    DOWNLOADING,

    @SerialName("forcedMetaDL")
    FORCED_META_DL,

    @SerialName("metaDL")
    META_DL,

    @SerialName("stalledDL")
    STALLED_DL,

    @SerialName("forcedUP")
    FORCED_UP,

    @SerialName("uploading")
    UPLOADING,

    @SerialName("stalledUP")
    STALLED_UP,

    @SerialName("checkingResumeData")
    CHECKING_RESUME_DATA,

    @SerialName("queuedDL")
    QUEUED_DL,

    @SerialName("queuedUP")
    QUEUED_UP,

    @SerialName("checkingUP")
    CHECKING_UP,

    @SerialName("checkingDL")
    CHECKING_DL,

    @SerialName("pausedDL")
    PAUSED_DL,

    @SerialName("pausedUP")
    PAUSED_UP,

    @SerialName("moving")
    MOVING,

    @SerialName("missingFiles")
    MISSING_FILES,

    @SerialName("error")
    ERROR,

    @SerialName("unknown")
    UNKNOWN,
}

private object TorrentStateSerializer : KSerializer<TorrentState> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TorrentState", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TorrentState) {
        TorrentState.generatedSerializer().serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): TorrentState {
        return when (decoder.decodeString()) {
            "pausedDL", "stoppedDL" -> TorrentState.PAUSED_DL
            "pausedUP", "stoppedUP" -> TorrentState.PAUSED_UP
            "forcedDL" -> TorrentState.FORCED_DL
            "downloading" -> TorrentState.DOWNLOADING
            "forcedMetaDL" -> TorrentState.FORCED_META_DL
            "metaDL" -> TorrentState.META_DL
            "stalledDL" -> TorrentState.STALLED_DL
            "forcedUP" -> TorrentState.FORCED_UP
            "uploading" -> TorrentState.UPLOADING
            "stalledUP" -> TorrentState.STALLED_UP
            "checkingResumeData" -> TorrentState.CHECKING_RESUME_DATA
            "queuedDL" -> TorrentState.QUEUED_DL
            "queuedUP" -> TorrentState.QUEUED_UP
            "checkingUP" -> TorrentState.CHECKING_UP
            "checkingDL" -> TorrentState.CHECKING_DL
            "moving" -> TorrentState.MOVING
            "missingFiles" -> TorrentState.MISSING_FILES
            "error" -> TorrentState.ERROR
            else -> TorrentState.UNKNOWN
        }
    }
}

@Serializable(with = PieceStateSerializer::class)
enum class PieceState(val id: Int) {
    NOT_DOWNLOADED(0),
    DOWNLOADING(1),
    DOWNLOADED(2),
}

private object PieceStateSerializer : KSerializer<PieceState> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PieceState", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: PieceState) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): PieceState {
        val pieceStateId = decoder.decodeInt()
        return PieceState.entries.find { it.id == pieceStateId }
            ?: throw IllegalArgumentException("Unknown PieceState id: $pieceStateId")
    }
}
