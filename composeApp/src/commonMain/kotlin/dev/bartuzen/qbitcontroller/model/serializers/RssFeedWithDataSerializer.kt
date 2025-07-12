package dev.bartuzen.qbitcontroller.model.serializers

import dev.bartuzen.qbitcontroller.model.Article
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
            val (currentNode, currentPath) = findNodeByUid(rootNode, uid) ?: return null to null
            node = currentNode
            newPath = currentPath
            break
        } else {
            return null to null
        }
    }

    parseRssFeedWithData(node, articles, newPath ?: path)
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

private val dateFormat = DateTimeComponents.Format {
    day()
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    year()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
    char(' ')
    offset(UtcOffset.Formats.FOUR_DIGITS)
}

private fun parseArticles(node: JsonElement, articles: MutableList<Article>, path: List<String>): List<Article> {
    for (article in node.jsonArray) {
        articles += Article(
            id = article.jsonObject["id"]?.jsonPrimitive?.content!!,
            title = article.jsonObject["title"]?.jsonPrimitive?.content!!,
            description = article.jsonObject["description"]?.jsonPrimitive?.content,
            torrentUrl = article.jsonObject["torrentURL"]?.jsonPrimitive?.content!!,
            isRead = article.jsonObject["isRead"]?.jsonPrimitive?.booleanOrNull == true,
            date = dateFormat.parse(article.jsonObject["date"]?.jsonPrimitive?.content!!).toInstantUsingOffset(),
            path = path,
        )
    }

    return articles
}

private fun findNodeByUid(node: JsonElement, uid: String): Pair<JsonElement, List<String>>? {
    val nodes = ArrayDeque<Pair<JsonElement, List<String>>>()
    nodes += node to emptyList()

    while (nodes.isNotEmpty()) {
        val (json, path) = nodes.removeLast()
        if (isFeed(json)) {
            if (json.jsonObject["uid"]?.jsonPrimitive?.content == uid) {
                return json to path
            }
        } else {
            for ((key, value) in json.jsonObject) {
                nodes += value to path + key
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
