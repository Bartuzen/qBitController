package dev.bartuzen.qbitcontroller.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.Parcelize

@Parcelize
data class TorrentProperties(
    @JsonProperty("piece_size") val pieceSize: Int,
    @JsonProperty("pieces_num") val piecesCount: Int
) : Parcelable