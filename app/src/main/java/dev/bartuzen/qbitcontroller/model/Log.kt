package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.LogTypeDeserializer

data class Log(
    val id: Int,
    val message: String,
    val timestamp: Long,
    @JsonDeserialize(using = LogTypeDeserializer::class)
    val type: LogType
)

enum class LogType {
    NORMAL, INFO, WARNING, CRITICAL
}
