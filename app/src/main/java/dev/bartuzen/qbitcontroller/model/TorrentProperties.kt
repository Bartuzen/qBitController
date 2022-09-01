package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class TorrentProperties(
    @JsonProperty("piece_size") val pieceSize: Int,
    @JsonProperty("pieces_num") val piecesCount: Int
)