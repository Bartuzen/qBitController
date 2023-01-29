package dev.bartuzen.qbitcontroller.model

data class RssFeedWithData(
    val name: String,
    val path: List<String>,
    val uid: String,
    val articles: List<Article>
)

data class Article(
    val id: String,
    val title: String,
    val description: String,
    val torrentUrl: String,
    val isRead: Boolean,
    val date: Long
)
