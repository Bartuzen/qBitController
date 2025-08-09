package dev.bartuzen.qbitcontroller.model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object NullableDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NullableDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double?) {
        encoder.encodeDouble(value ?: -1.0)
    }

    override fun deserialize(decoder: Decoder): Double? {
        return decoder.decodeDouble().takeIf { it != -1.0 }
    }
}
