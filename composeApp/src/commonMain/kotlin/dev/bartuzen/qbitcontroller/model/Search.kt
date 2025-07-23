package dev.bartuzen.qbitcontroller.model

import dev.bartuzen.qbitcontroller.model.serializers.NullableIntSerializer
import dev.bartuzen.qbitcontroller.model.serializers.NullableLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Search(
    val status: Status,
    val results: List<Result>,
    val total: Int,
) {
    @Serializable
    data class Result(
        @SerialName("descrLink")
        val descriptionLink: String,

        val fileName: String,

        @Serializable(with = NullableLongSerializer::class)
        val fileSize: Long?,

        val fileUrl: String,

        @SerialName("nbLeechers")
        @Serializable(with = NullableIntSerializer::class)
        val leechers: Int?,

        @SerialName("nbSeeders")
        @Serializable(with = NullableIntSerializer::class)
        val seeders: Int?,

        val siteUrl: String,
    )

    enum class Status {
        @SerialName("Running")
        RUNNING,

        @SerialName("Stopped")
        STOPPED,
    }
}
