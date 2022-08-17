package dev.bartuzen.qbitcontroller.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.Parcelize

@Parcelize
data class TorrentFile(
    @JsonProperty("index") val index: Int,
    @JsonProperty("name") val name: String
) : Parcelable