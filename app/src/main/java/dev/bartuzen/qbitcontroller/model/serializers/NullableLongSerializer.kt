package dev.bartuzen.qbitcontroller.model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object NullableLongSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NullableLong", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Long?) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): Long? {
        return decoder.decodeLong().takeIf { it != -1L }
    }
}
