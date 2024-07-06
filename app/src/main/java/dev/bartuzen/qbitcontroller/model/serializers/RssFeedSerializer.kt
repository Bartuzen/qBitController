package dev.bartuzen.qbitcontroller.model.serializers

import dev.bartuzen.qbitcontroller.model.RssFeed
import dev.bartuzen.qbitcontroller.model.RssFeedNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun parseRssFeeds(feeds: String): RssFeedNode {
    val node = Json.parseToJsonElement(feeds)
    val feedNode = RssFeedNode("/", null, mutableListOf(), listOf(), 0)

    parseRssFeeds(node, feedNode, 1)
    sortNodes(feedNode)
    return feedNode
}

private fun parseRssFeeds(node: JsonElement, feedNode: RssFeedNode, level: Int) {
    for ((key, value) in node.jsonObject) {
        if (isFeed(value)) {
            feedNode.children!!.add(
                RssFeedNode(
                    name = key,
                    feed = RssFeed(
                        name = key,
                        uid = value.jsonObject["uid"]?.jsonPrimitive?.content ?: "",
                        url = value.jsonObject["url"]?.jsonPrimitive?.content ?: "",
                    ),
                    children = null,
                    path = feedNode.path + key,
                    level = level,
                ),
            )
        } else {
            val childNode = RssFeedNode(
                name = key,
                feed = null,
                children = mutableListOf(),
                path = feedNode.path + key,
                level = level,
            )
            feedNode.children!!.add(childNode)
            parseRssFeeds(value, childNode, level + 1)
        }
    }
}

private fun isFeed(node: JsonElement): Boolean {
    for ((key, value) in node.jsonObject) {
        if (key == "uid" && value.jsonPrimitive.isString) {
            return true
        }
    }
    return false
}

private fun sortNodes(node: RssFeedNode) {
    node.children?.let { children ->
        children.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        children.forEach { childNode ->
            sortNodes(childNode)
        }
    }
}
