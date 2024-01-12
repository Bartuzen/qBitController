package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.CategoryDeserializer

@JsonDeserialize(using = CategoryDeserializer::class)
data class Category(
    val name: String,
    val savePath: String,
    val downloadPath: DownloadPath
) {
    sealed interface DownloadPath {
        object Default : DownloadPath {
            override fun toString() = "dev.bartuzen.qbittorrent.models.Category.DownloadPath.Default"
        }

        object No : DownloadPath {
            override fun toString() = "dev.bartuzen.qbittorrent.models.Category.DownloadPath.No"
        }

        data class Yes(val path: String) : DownloadPath
    }
}
