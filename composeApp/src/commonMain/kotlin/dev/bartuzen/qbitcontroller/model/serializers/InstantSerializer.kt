package dev.bartuzen.qbitcontroller.model.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }

    override fun deserialize(decoder: Decoder): Instant {
        val value = decoder.decodeLong()
        return if (value >= 100_000_000_000) {
            Instant.fromEpochMilliseconds(value)
        } else {
            Instant.fromEpochSeconds(value)
        }
    }
}

object NullableInstantSerializer : KSerializer<Instant?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant?) {
        if (value != null) {
            encoder.encodeLong(value.toEpochMilliseconds())
        } else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): Instant? {
        val value = decoder.decodeLong()
        return if (value < 24 * 3600) {
            null
        } else if (value >= 100_000_000_000) {
            Instant.fromEpochMilliseconds(value)
        } else {
            Instant.fromEpochSeconds(value)
        }
    }
}
