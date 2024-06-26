package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Category(
    val name: String,
    val savePath: String,
    @SerialName("download_path")
    @Serializable(with = DownloadPathSerializer::class)
    val downloadPath: DownloadPath = DownloadPath.Default
) {
    sealed interface DownloadPath {
        data object Default : DownloadPath
        data object No : DownloadPath
        data class Yes(val path: String) : DownloadPath
    }
}

private object DownloadPathSerializer : KSerializer<Category.DownloadPath> {
    override val descriptor = JsonPrimitive.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Category.DownloadPath) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): Category.DownloadPath {
        val jsonValue = (decoder as JsonDecoder).decodeJsonElement().jsonPrimitive
        return if (jsonValue.isString) {
            Category.DownloadPath.Yes(jsonValue.content)
        } else {
            Category.DownloadPath.No
        }
    }
}
