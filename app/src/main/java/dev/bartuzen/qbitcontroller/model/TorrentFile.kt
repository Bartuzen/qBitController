package dev.bartuzen.qbitcontroller.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.Parcelize

@JsonIgnoreProperties(ignoreUnknown = true)
@Parcelize
data class TorrentFile(
    @JsonProperty("index") val index: Int,
    @JsonProperty("name") val name: String
) : Parcelable