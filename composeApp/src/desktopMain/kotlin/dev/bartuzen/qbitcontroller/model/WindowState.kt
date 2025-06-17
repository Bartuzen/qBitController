package dev.bartuzen.qbitcontroller.model

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable(with = WindowStateSerializer::class)
data class WindowState(
    val placement: WindowPlacement = WindowPlacement.Floating,
    val position: WindowPosition = WindowPosition.PlatformDefault,
    val size: DpSize = DpSize(800.dp, 600.dp),
)

private object WindowStateSerializer : KSerializer<WindowState> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("WindowState") {
        element<String>("placement")
        element<Double?>("positionX")
        element<Double?>("positionY")
        element<Float>("width")
        element<Float>("height")
    }

    override fun serialize(encoder: Encoder, value: WindowState) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.placement.name)

            when (val position = value.position) {
                is WindowPosition.Absolute -> {
                    encodeNullableSerializableElement(descriptor, 1, Double.serializer(), position.x.value.toDouble())
                    encodeNullableSerializableElement(descriptor, 2, Double.serializer(), position.y.value.toDouble())
                }
                else -> {
                    encodeNullableSerializableElement(descriptor, 1, Double.serializer(), null)
                    encodeNullableSerializableElement(descriptor, 2, Double.serializer(), null)
                }
            }

            encodeFloatElement(descriptor, 3, value.size.width.value)
            encodeFloatElement(descriptor, 4, value.size.height.value)
        }
    }

    override fun deserialize(decoder: Decoder): WindowState {
        return decoder.decodeStructure(descriptor) {
            var placement = WindowPlacement.Floating
            var posX: Double? = null
            var posY: Double? = null
            var width = 800f
            var height = 600f

            while (true) {
                when (decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> placement = WindowPlacement.valueOf(decodeStringElement(descriptor, 0))
                    1 -> posX = decodeNullableSerializableElement(descriptor, 1, Double.serializer())
                    2 -> posY = decodeNullableSerializableElement(descriptor, 2, Double.serializer())
                    3 -> width = decodeFloatElement(descriptor, 3)
                    4 -> height = decodeFloatElement(descriptor, 4)
                }
            }

            val position = if (posX != null && posY != null) {
                WindowPosition.Absolute(posX.dp, posY.dp)
            } else {
                WindowPosition.PlatformDefault
            }

            WindowState(
                placement = placement,
                position = position,
                size = DpSize(width.dp, height.dp),
            )
        }
    }
}
