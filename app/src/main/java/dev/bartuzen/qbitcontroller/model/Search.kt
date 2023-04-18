package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.NullableIntDeserializer
import dev.bartuzen.qbitcontroller.model.deserializers.NullableLongDeserializer

data class Search(
    val status: Status,
    val total: Int,
    val results: List<Result>
) {
    data class Result(
        @JsonProperty("descrLink")
        val descriptionLink: String,

        val fileName: String,

        @JsonDeserialize(using = NullableLongDeserializer::class)
        val fileSize: Long?,

        val fileUrl: String,

        @JsonProperty("nbLeechers")
        @JsonDeserialize(using = NullableIntDeserializer::class)
        val leechers: Int?,

        @JsonProperty("nbSeeders")
        @JsonDeserialize(using = NullableIntDeserializer::class)
        val seeders: Int?,

        val siteUrl: String
    )

    enum class Status {
        @JsonProperty("Running")
        RUNNING,

        @JsonProperty("Stopped")
        STOPPED
    }
}
