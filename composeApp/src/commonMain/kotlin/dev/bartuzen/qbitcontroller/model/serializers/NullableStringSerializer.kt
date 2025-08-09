package dev.bartuzen.qbitcontroller.model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object NullableStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NullableString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        return encoder.encodeString(value ?: "")
    }

    override fun deserialize(decoder: Decoder): String? {
        return decoder.decodeString().ifEmpty { null }
    }
}
