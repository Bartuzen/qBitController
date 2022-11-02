package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Category(
    @JsonProperty("name")
    val name: String
)
