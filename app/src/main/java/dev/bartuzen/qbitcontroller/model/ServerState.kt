package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ServerState(
    @JsonProperty("dl_info_data")
    val downloadSession: Long,

    @JsonProperty("dl_info_speed")
    val downloadSpeed: Int,

    @JsonProperty("up_info_data")
    val uploadSession: Long,

    @JsonProperty("up_info_speed")
    val uploadSpeed: Int,

    @JsonProperty("use_alt_speed_limits")
    val useAlternativeSpeedLimits: Boolean
)
