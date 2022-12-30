package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.exc.InputCoercionException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class EtaDeserializer : JsonDeserializer<Int?>() {
    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): Int? {
        return try {
            parser?.intValue.takeIf { it != null && (it in 0..8640000) }?.toInt()
        } catch (_: InputCoercionException) {
            null
        }
    }
}
