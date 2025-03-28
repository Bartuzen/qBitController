package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.Serializable

@Serializable
data class RssFeed(
    val name: String,
    val uid: String,
    val url: String,
)
