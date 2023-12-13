package dev.bartuzen.qbitcontroller.model.serializers

import dev.bartuzen.qbitcontroller.model.RssFeed
import dev.bartuzen.qbitcontroller.model.RssFeedNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun parseRssFeeds(feeds: String): RssFeedNode {
    val node = Json.parseToJsonElement(feeds)
    val feedNode = RssFeedNode("", null, mutableListOf())

    parseRssFeeds(node, feedNode)
    return feedNode
}

private fun parseRssFeeds(node: JsonElement, feedNode: RssFeedNode) {
    for ((key, value) in node.jsonObject) {
        if (isFeed(value)) {
            feedNode.children!!.add(
                RssFeedNode(
                    name = key,
                    feed = RssFeed(
                        name = key,
                        uid = value.jsonObject["uid"]?.jsonPrimitive?.content ?: "",
                        url = value.jsonObject["url"]?.jsonPrimitive?.content ?: ""
                    ),
                    children = null
                )
            )
        } else {
            val childNode = RssFeedNode(key, null, mutableListOf())
            feedNode.children!!.add(childNode)
            parseRssFeeds(value, childNode)
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
