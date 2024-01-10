package dev.bartuzen.qbitcontroller.model.serializers

import dev.bartuzen.qbitcontroller.model.Article
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parseRssFeedWithData(feeds: String, path: List<String>): List<Article>? {
    var node = Json.parseToJsonElement(feeds)
    val articles = mutableListOf<Article>()

    path.forEach { name ->
        node = node.jsonObject[name] ?: return null
    }

    parseRssFeedWithData(node, articles)
    articles.sortByDescending { it.date }
    return articles
}

private fun parseRssFeedWithData(node: JsonElement, articles: MutableList<Article>) {
    if (isFeed(node)) {
        parseArticles(node.jsonObject["articles"]!!, articles)
    } else {
        for ((_, value) in node.jsonObject) {
            parseRssFeedWithData(value, articles)
        }
    }
}

private fun parseArticles(node: JsonElement, articles: MutableList<Article>): List<Article> {
    for (article in node.jsonArray) {
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        val date = ZonedDateTime.parse(article.jsonObject["date"]?.jsonPrimitive?.content, dateFormatter).toEpochSecond()

        articles += Article(
            id = article.jsonObject["id"]?.jsonPrimitive?.content!!,
            title = article.jsonObject["title"]?.jsonPrimitive?.content!!,
            description = article.jsonObject["description"]?.jsonPrimitive?.content,
            torrentUrl = article.jsonObject["torrentURL"]?.jsonPrimitive?.content!!,
            isRead = article.jsonObject["isRead"]?.jsonPrimitive?.booleanOrNull ?: false,
            date = date
        )
    }

    return articles
}

private fun isFeed(node: JsonElement): Boolean {
    for ((key, value) in node.jsonObject) {
        if (key == "articles" && value is JsonArray) {
            return true
        }
    }
    return false
}
