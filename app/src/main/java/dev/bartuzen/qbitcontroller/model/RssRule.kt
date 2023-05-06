package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class RssRule(
    val name: String,
    @JsonProperty("enabled")
    val isEnabled: Boolean
)
