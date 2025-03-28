package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.Serializable

@Serializable
data class RssFeedNode(
    val name: String,
    val feed: RssFeed?,
    val children: MutableList<RssFeedNode>?,
    val path: List<String>,
    val level: Int,
) {
    val isFeed get() = children == null

    val isFolder get() = children != null

    val uniqueId = feed?.uid ?: "$level-$name"
}
