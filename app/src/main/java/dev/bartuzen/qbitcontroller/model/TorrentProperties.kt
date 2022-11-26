package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.NullableIntDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.NullableLongDeserializer

data class TorrentProperties(
    @JsonProperty("piece_size")
    @JsonDeserialize(using = NullableLongDeserializer::class)
    val pieceSize: Long?,

    @JsonProperty("pieces_num")
    @JsonDeserialize(using = NullableIntDeserializer::class)
    val piecesCount: Int?
)
