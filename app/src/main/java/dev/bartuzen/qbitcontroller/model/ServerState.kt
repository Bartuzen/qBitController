package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ServerState(
    @JsonProperty("use_alt_speed_limits")
    val useAlternativeSpeedLimits: Boolean
)
