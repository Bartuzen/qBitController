package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bartuzen.qbitcontroller.model.RssFeed

fun parseRssFeeds(feeds: String): List<RssFeed> {
    val mapper = jacksonObjectMapper()
    val node = mapper.readTree(feeds)
    return parseRssFeeds(node, emptyList())
}

private fun parseRssFeeds(node: JsonNode, path: List<String>): List<RssFeed> {
    val feeds = mutableListOf<RssFeed>()

    val fields = node.fields()

    for ((key, value) in fields) {
        if (isFeed(value)) {
            feeds.add(RssFeed(key, path, value["uid"].asText()))
        } else {
            feeds += parseRssFeeds(value, path + key)
        }
    }

    return feeds
}

private fun isFeed(node: JsonNode): Boolean {
    for ((key, value) in node.fields()) {
        if (key == "uid" && value.isTextual) {
            return true
        }
    }
    return false
}
