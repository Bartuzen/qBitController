package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bartuzen.qbitcontroller.model.Article
import dev.bartuzen.qbitcontroller.model.RssFeedWithData
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parseRssFeedWithData(feeds: String, path: List<String>): RssFeedWithData? {
    val mapper = jacksonObjectMapper()
    val node = mapper.readTree(feeds)
    return parseRssFeedWithData(node, path)
}

private fun parseRssFeedWithData(node: JsonNode, path: List<String>): RssFeedWithData? {
    var currentNode = node
    var name = ""
    outer@ for (currentPath in path) {
        for ((key, value) in currentNode.fields()) {
            if (key == currentPath) {
                name = key
                currentNode = value
                continue@outer
            }
        }
        return null
    }

    return RssFeedWithData(
        name = name,
        path = path,
        uid = currentNode["uid"].asText(),
        articles = parseArticles(currentNode["articles"])
    )
}

private fun parseArticles(node: JsonNode): List<Article> {
    val articles = mutableListOf<Article>()

    for (article in node.iterator()) {
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        val date = LocalDateTime.parse(article["date"].asText(), dateFormatter).toEpochSecond(ZoneOffset.UTC)

        articles += Article(
            id = article["id"].asText(),
            title = article["title"].asText(),
            description = article["description"].asText(),
            torrentUrl = article["torrentURL"].asText(),
            date = date
        )
    }

    articles.sortByDescending { it.date }

    return articles
}
