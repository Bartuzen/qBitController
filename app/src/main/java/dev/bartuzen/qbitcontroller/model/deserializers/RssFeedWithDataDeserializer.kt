package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bartuzen.qbitcontroller.model.Article
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parseRssFeedWithData(feeds: String, path: List<String>): List<Article>? {
    val mapper = jacksonObjectMapper()
    var node = mapper.readTree(feeds)
    val articles = mutableListOf<Article>()

    path.forEach { name ->
        node = node[name] ?: return null
    }

    parseRssFeedWithData(node, articles)
    articles.sortByDescending { it.date }
    return articles
}

private fun parseRssFeedWithData(node: JsonNode, articles: MutableList<Article>) {
    if (isFeed(node)) {
        parseArticles(node["articles"], articles)
    } else {
        for ((_, value) in node.fields()) {
            parseRssFeedWithData(value, articles)
        }
    }
}

private fun parseArticles(node: JsonNode, articles: MutableList<Article>): List<Article> {
    for (article in node.iterator()) {
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        val date = ZonedDateTime.parse(article["date"].asText(), dateFormatter).toEpochSecond()

        articles += Article(
            id = article["id"].asText(),
            title = article["title"].asText(),
            description = article["description"]?.asText(),
            torrentUrl = article["torrentURL"].asText(),
            isRead = article["isRead"]?.asBoolean() ?: false,
            date = date
        )
    }

    return articles
}

private fun isFeed(node: JsonNode): Boolean {
    for ((key, value) in node.fields()) {
        if (key == "articles" && value.isArray) {
            return true
        }
    }
    return false
}
