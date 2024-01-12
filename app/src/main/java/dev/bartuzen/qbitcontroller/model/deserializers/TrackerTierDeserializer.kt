package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class TrackerTierDeserializer : JsonDeserializer<Int?>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Int? {
        return if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            parser.intValue.takeIf { it != -1 }
        } else {
            null
        }
    }
}
