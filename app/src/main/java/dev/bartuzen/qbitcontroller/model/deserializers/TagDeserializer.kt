package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class TagDeserializer : JsonDeserializer<List<String>?>() {
    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): List<String>? {
        val tags = parser?.codec?.readValue(parser, String::class.java) ?: return null
        return if (tags.isNotEmpty()) tags.split(",") else emptyList()
    }
}