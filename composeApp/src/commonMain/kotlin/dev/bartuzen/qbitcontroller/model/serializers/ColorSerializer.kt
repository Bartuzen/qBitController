package dev.bartuzen.qbitcontroller.model.serializers

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ColorSerializer : KSerializer<Color> {
    override val descriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        val argb = value.value.toString(16)
        encoder.encodeString(argb)
    }

    override fun deserialize(decoder: Decoder): Color {
        val argb = decoder.decodeString().toULong(16)
        return Color(argb)
    }
}
