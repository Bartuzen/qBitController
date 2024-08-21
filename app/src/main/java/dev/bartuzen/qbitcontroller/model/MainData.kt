package dev.bartuzen.qbitcontroller.model

import dev.bartuzen.qbitcontroller.utils.formatUri
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

@Serializable(with = MainDataSerializer::class)
data class MainData(
    val serverState: ServerState,
    val torrents: List<Torrent>,
    val categories: List<Category>,
    val tags: List<String>,
    val trackers: Map<String, List<String>>,
)

private object MainDataSerializer : KSerializer<MainData> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("MainData") {
            element<ServerState>("server_state")
            element("torrents", mapSerialDescriptor<String, Torrent>())
            element("categories", mapSerialDescriptor<String, Category>())
            element("tags", listSerialDescriptor<String>())
            element("trackers", mapSerialDescriptor<String, List<String>>())
        }

    override fun serialize(encoder: Encoder, value: MainData) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        var decodedServerState: ServerState? = null
        var decodedTorrents: Map<String, Torrent>? = null
        var decodedCategories: Map<String, Category>? = null
        var decodedTags: List<String>? = null
        var decodedTrackers: Map<String, List<String>>? = null

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> decodedServerState = decodeSerializableElement(
                    descriptor,
                    index,
                    ServerState.serializer(),
                )
                1 -> decodedTorrents = decodeSerializableElement(
                    descriptor,
                    index,
                    MapSerializer(String.serializer(), Torrent.serializer()),
                )
                2 -> decodedCategories = decodeSerializableElement(
                    descriptor,
                    index,
                    MapSerializer(String.serializer(), Category.serializer()),
                )
                3 -> decodedTags = decodeSerializableElement(
                    descriptor,
                    index,
                    ListSerializer(String.serializer()),
                )
                4 -> decodedTrackers = decodeSerializableElement(
                    descriptor,
                    index,
                    MapSerializer(String.serializer(), ListSerializer(String.serializer())),
                )
            }
        }

        val serverState = decodedServerState!!

        val torrents = decodedTorrents?.mapValues { (hash, torrent) ->
            torrent.copy(hash = hash)
        }?.values?.toList() ?: emptyList()

        val categoryComparator = Comparator<Category> { category1, category2 ->
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

        val categories = decodedCategories?.values?.sortedWith(
            if (serverState.areSubcategoriesEnabled) subcategoryComparator else categoryComparator,
        ) ?: emptyList()

        val tags = decodedTags?.sortedWith(String.CASE_INSENSITIVE_ORDER) ?: emptyList()

        val trackers = sortedMapOf<String, MutableList<String>>()
        decodedTrackers?.forEach { (tracker, hashes) ->
            val formattedTracker = formatUri(tracker)
            val list = trackers.getOrPut(formattedTracker) { mutableListOf() }
            list.addAll(hashes)
        }

        MainData(
            serverState = serverState,
            torrents = torrents,
            categories = categories,
            tags = tags,
            trackers = trackers,
        )
    }
}
