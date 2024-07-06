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

fun parseRssFeedWithData(feeds: String, path: List<String>, uid: String?): Pair<List<Article>?, List<String>?> {
    val rootNode = Json.parseToJsonElement(feeds)
    var node = rootNode
    val articles = mutableListOf<Article>()
    var newPath: List<String>? = null

    for (name in path) {
        val newNode = node.jsonObject[name]
        if (newNode != null) {
            node = newNode
        } else if (uid != null) {
            val uidNode = findNodeByUid(rootNode, uid)

            if (uidNode != null) {
                node = uidNode.first
                newPath = uidNode.second
                break
            } else {
                return null to null
            }
        } else {
            return null to null
        }
    }

    parseRssFeedWithData(node, articles, path)
    articles.sortByDescending { it.date }
    return articles to newPath
}

private fun parseRssFeedWithData(node: JsonElement, articles: MutableList<Article>, path: List<String>) {
    if (isFeed(node)) {
        parseArticles(node.jsonObject["articles"]!!, articles, path)
    } else {
        for ((key, value) in node.jsonObject) {
            val newPath = path + key
            parseRssFeedWithData(value, articles, newPath)
        }
    }
}

private fun parseArticles(node: JsonElement, articles: MutableList<Article>, path: List<String>): List<Article> {
    for (article in node.jsonArray) {
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        val date = ZonedDateTime.parse(article.jsonObject["date"]?.jsonPrimitive?.content, dateFormatter).toEpochSecond()

        articles += Article(
            id = article.jsonObject["id"]?.jsonPrimitive?.content!!,
            title = article.jsonObject["title"]?.jsonPrimitive?.content!!,
            description = article.jsonObject["description"]?.jsonPrimitive?.content,
            torrentUrl = article.jsonObject["torrentURL"]?.jsonPrimitive?.content!!,
            isRead = article.jsonObject["isRead"]?.jsonPrimitive?.booleanOrNull ?: false,
            date = date,
            path = path,
        )
    }

    return articles
}

private fun findNodeByUid(node: JsonElement, uid: String): Pair<JsonElement, List<String>>? {
    val nodes = ArrayDeque<Pair<JsonElement, List<String>>>()
    nodes += node to emptyList()

    while (nodes.isNotEmpty()) {
        val currentNode = nodes.removeLast()
        if (isFeed(currentNode.first)) {
            if (currentNode.first.jsonObject["uid"]?.jsonPrimitive?.content == uid) {
                return currentNode
            }
        } else {
            for ((key, value) in currentNode.first.jsonObject) {
                nodes += value to currentNode.second + key
            }
        }
    }

    return null
}

private fun isFeed(node: JsonElement): Boolean {
    for ((key, value) in node.jsonObject) {
        if (key == "articles" && value is JsonArray) {
            return true
        }
    }
    return false
}
