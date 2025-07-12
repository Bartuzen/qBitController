package dev.bartuzen.qbitcontroller.model

import kotlin.time.Instant

data class Article(
    val id: String,
    val title: String,
    val description: String?,
    val torrentUrl: String,
    val isRead: Boolean,
    val date: Instant,
    val path: List<String>,
)
