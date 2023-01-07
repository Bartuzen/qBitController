package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import dev.bartuzen.qbitcontroller.model.PeerFlag

class PeerFlagDeserializer : JsonDeserializer<List<PeerFlag>>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): List<PeerFlag> {
        return p?.valueAsString?.split(" ")?.mapNotNull { flagStr ->
            PeerFlag.values().find { it.flag == flagStr }
        } ?: emptyList()
    }
}
