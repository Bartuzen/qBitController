package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import dev.bartuzen.qbitcontroller.model.LogType

class LogTypeDeserializer : JsonDeserializer<LogType>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): LogType {
        return when (val value = parser.valueAsInt) {
            1 -> LogType.NORMAL
            2 -> LogType.INFO
            4 -> LogType.WARNING
            8 -> LogType.CRITICAL
            else -> throw IllegalStateException("Unknown log type: $value")
        }
    }
}
