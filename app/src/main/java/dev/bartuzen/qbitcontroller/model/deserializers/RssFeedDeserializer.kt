package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bartuzen.qbitcontroller.model.RssFeed
import dev.bartuzen.qbitcontroller.model.RssFeedNode

fun parseRssFeeds(feeds: String): RssFeedNode {
    val mapper = jacksonObjectMapper()
    val node = mapper.readTree(feeds)
    val feedNode = RssFeedNode("/", null, mutableListOf(), listOf(), 0)

    parseRssFeeds(node, feedNode, 1)
    sortNodes(feedNode)
    return feedNode
}

private fun parseRssFeeds(node: JsonNode, feedNode: RssFeedNode, level: Int) {
    for ((key, value) in node.fields()) {
        if (isFeed(value)) {
            feedNode.children!!.add(
                RssFeedNode(
                    name = key,
                    feed = RssFeed(
                        name = key,
                        uid = value["uid"].asText(),
                        url = value["url"].asText()
                    ),
                    children = null,
                    path = feedNode.path + key,
                    level = level
                )
            )
        } else {
            val childNode = RssFeedNode(
                name = key,
                feed = null,
                children = mutableListOf(),
                path = feedNode.path + key,
                level = level
            )
            feedNode.children!!.add(childNode)
            parseRssFeeds(value, childNode, level + 1)
        }
    }
}

private fun isFeed(node: JsonNode): Boolean {
    for ((key, value) in node.fields()) {
        if (key == "uid" && value.isTextual) {
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
