package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bartuzen.qbitcontroller.model.RssFeed
import dev.bartuzen.qbitcontroller.model.RssFeedNode

fun parseRssFeeds(feeds: String): RssFeedNode {
    val mapper = jacksonObjectMapper()
    val node = mapper.readTree(feeds)
    val feedNode = RssFeedNode("", null, mutableListOf())

    parseRssFeeds(node, feedNode)
    return feedNode
}

private fun parseRssFeeds(node: JsonNode, feedNode: RssFeedNode) {
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

private fun isFeed(node: JsonNode): Boolean {
    for ((key, value) in node.fields()) {
        if (key == "uid" && value.isTextual) {
            return true
        }
    }
    return false
}
