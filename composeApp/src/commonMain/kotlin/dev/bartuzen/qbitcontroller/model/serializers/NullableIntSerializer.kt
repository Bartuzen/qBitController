package dev.bartuzen.qbitcontroller.model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object NullableIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NullableInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int?) {
        encoder.encodeInt(value ?: -1)
    }

    override fun deserialize(decoder: Decoder): Int? {
        return decoder.decodeInt().takeIf { it != -1 }
    }
}
