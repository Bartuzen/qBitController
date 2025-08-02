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
    val downloadPath: DownloadPath = DownloadPath.Default,
) {
    sealed interface DownloadPath {
        data object Default : DownloadPath
        data object No : DownloadPath
        data class Yes(val path: String) : DownloadPath
    }

    companion object {
        val comparator = Comparator<Category> { category1, category2 ->
            category1.name.compareTo(category2.name, ignoreCase = true).let { comparison ->
                if (comparison != 0) {
                    return@Comparator comparison
                }
            }

            category1.name.compareTo(category2.name)
        }

        val subcategoryComparator = Comparator<Category> { category1, category2 ->
            val parts1 = category1.name.split("/")
            val parts2 = category2.name.split("/")

            for (i in parts1.indices) {
                if (i >= parts2.size) {
                    return@Comparator 1
                }

                val part1 = parts1[i]
                val part2 = parts2[i]

                part1.compareTo(part2, ignoreCase = true).let { comparison ->
                    if (comparison != 0) {
                        return@Comparator comparison
                    }
                }

                part1.compareTo(part2).let { comparison ->
                    if (comparison != 0) {
                        return@Comparator comparison
                    }
                }
            }

            return@Comparator parts1.size.compareTo(parts2.size)
        }
    }
}

private object DownloadPathSerializer : KSerializer<Category.DownloadPath> {
    override val descriptor = JsonPrimitive.serializer().descriptor

    // Gets called, but the result won't be used
    override fun serialize(encoder: Encoder, value: Category.DownloadPath) {
        encoder.encodeNull()
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
