package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RssRule(
    @SerialName("enabled") val isEnabled: Boolean,
    val mustContain: String,
    val mustNotContain: String,
    val useRegex: Boolean,
    val episodeFilter: String,
    val ignoreDays: Int,
    val addPaused: Boolean?,
    val assignedCategory: String,
    val savePath: String,
    val torrentContentLayout: String?,
    val smartFilter: Boolean,
    val affectedFeeds: List<String>,
)
