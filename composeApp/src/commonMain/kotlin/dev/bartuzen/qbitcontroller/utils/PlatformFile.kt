package dev.bartuzen.qbitcontroller.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

expect fun String.toPlatformFile(): PlatformFile

object PlatformFileSerializer : KSerializer<PlatformFile> {
    override val descriptor = PrimitiveSerialDescriptor("PlatformFile", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PlatformFile) {
        encoder.encodeString(value.path)
    }

    override fun deserialize(decoder: Decoder): PlatformFile {
        return decoder.decodeString().toPlatformFile()
    }
}
