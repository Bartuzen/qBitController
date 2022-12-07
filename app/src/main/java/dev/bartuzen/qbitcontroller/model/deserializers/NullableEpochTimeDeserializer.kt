package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class NullableEpochTimeDeserializer : JsonDeserializer<Long?>() {
    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): Long? {
        return parser?.longValue.takeIf { it != null && it >= 0 }
    }
}
