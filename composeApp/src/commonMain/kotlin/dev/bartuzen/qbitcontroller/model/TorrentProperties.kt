package dev.bartuzen.qbitcontroller.model

import dev.bartuzen.qbitcontroller.model.serializers.InstantSerializer
import dev.bartuzen.qbitcontroller.model.serializers.NullableInstantSerializer
import dev.bartuzen.qbitcontroller.model.serializers.NullableIntSerializer
import dev.bartuzen.qbitcontroller.model.serializers.NullableLongSerializer
import dev.bartuzen.qbitcontroller.model.serializers.NullableStringSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorrentProperties(
    @SerialName("piece_size")
    @Serializable(with = NullableLongSerializer::class)
    val pieceSize: Long?,

    @SerialName("pieces_num")
    @Serializable(with = NullableIntSerializer::class)
    val piecesCount: Int?,

    @SerialName("pieces_have")
    val piecesHave: Int,

    @SerialName("total_size")
    @Serializable(with = NullableLongSerializer::class)
    val totalSize: Long?,

    @SerialName("addition_date")
    @Serializable(with = InstantSerializer::class)
    val additionDate: Instant,

    @SerialName("completion_date")
    @Serializable(with = NullableInstantSerializer::class)
    val completionDate: Instant?,

    @SerialName("creation_date")
    @Serializable(with = NullableInstantSerializer::class)
    val creationDate: Instant?,

    @SerialName("created_by")
    @Serializable(with = NullableStringSerializer::class)
    val createdBy: String?,

    @SerialName("save_path")
    val savePath: String,

    @SerialName("comment")
    @Serializable(with = NullableStringSerializer::class)
    val comment: String?,

    @SerialName("reannounce")
    val nextReannounce: Long,

    @SerialName("nb_connections")
    val connections: Int,

    @SerialName("nb_connections_limit")
    val connectionsLimit: Int,

    @SerialName("seeds")
    val seeds: Int,

    @SerialName("seeds_total")
    val seedsTotal: Int,

    @SerialName("peers")
    val peers: Int,

    @SerialName("peers_total")
    val peersTotal: Int,

    @SerialName("total_wasted")
    val wasted: Long,
)
