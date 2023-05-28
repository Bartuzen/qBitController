package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class RssRule(
    @JsonProperty("enabled") val isEnabled: Boolean,
    val mustContain: String,
    val mustNotContain: String,
    val useRegex: Boolean,
    val episodeFilter: String,
    val ignoreDays: Int,
    val addPaused: Boolean?,
    val assignedCategory: String,
    val savePath: String,
    val torrentContentLayout: String?,
    val smartFilter: Boolean
)
