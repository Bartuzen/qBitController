package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Plugin(
    @JsonProperty("enabled")
    val isEnabled: Boolean,
    val fullName: String,
    val name: String,
    val url: String,
    val version: String
)
