package dev.bartuzen.qbitcontroller.model

data class RssFeedNode(
    val name: String,
    val feed: RssFeed?,
    var children: MutableList<RssFeedNode>?
) {
    val isFeed get() = children == null

    val isFolder get() = children != null

    fun findFolder(folderList: ArrayDeque<String>): RssFeedNode? {
        var currentNode = this
        for (child in folderList) {
            currentNode = currentNode.children?.find { it.name == child } ?: return null
        }
        return currentNode
    }
}
