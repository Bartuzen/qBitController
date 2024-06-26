package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Plugin(
    @SerialName("enabled")
    val isEnabled: Boolean,
    val fullName: String,
    val name: String,
    val url: String,
    val version: String
)
