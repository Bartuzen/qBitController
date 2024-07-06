package dev.bartuzen.qbitcontroller.model

data class RssFeedNode(
    val name: String,
    val feed: RssFeed?,
    val children: MutableList<RssFeedNode>?,
    val path: List<String>,
    val level: Int,
) {
    val isFeed get() = children == null

    val isFolder get() = children != null

    val uniqueId = "$isFeed-$level-$name"
}
