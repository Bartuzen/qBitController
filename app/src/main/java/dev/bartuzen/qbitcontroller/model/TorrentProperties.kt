package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.NullableEpochTimeDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.NullableIntDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.NullableLongDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.NullableStringDeserializer

data class TorrentProperties(
    @JsonProperty("piece_size")
    @JsonDeserialize(using = NullableLongDeserializer::class)
    val pieceSize: Long?,

    @JsonProperty("pieces_num")
    @JsonDeserialize(using = NullableIntDeserializer::class)
    val piecesCount: Int?,

    @JsonProperty("pieces_have")
    val piecesHave: Int,

    @JsonProperty("total_size")
    @JsonDeserialize(using = NullableLongDeserializer::class)
    val totalSize: Long?,

    @JsonProperty("addition_date")
    val additionDate: Long,

    @JsonProperty("completion_date")
    @JsonDeserialize(using = NullableEpochTimeDeserializer::class)
    val completionDate: Long?,

    @JsonProperty("creation_date")
    @JsonDeserialize(using = NullableEpochTimeDeserializer::class)
    val creationDate: Long?,

    @JsonProperty("created_by")
    @JsonDeserialize(using = NullableStringDeserializer::class)
    val createdBy: String?,

    @JsonProperty("save_path")
    val savePath: String,

    @JsonProperty("comment")
    @JsonDeserialize(using = NullableStringDeserializer::class)
    val comment: String?,
)
