package dev.bartuzen.qbitcontroller.model

data class Article(
    val id: String,
    val title: String,
    val description: String?,
    val torrentUrl: String,
    val isRead: Boolean,
    val date: Long,
    val path: List<String>
)
