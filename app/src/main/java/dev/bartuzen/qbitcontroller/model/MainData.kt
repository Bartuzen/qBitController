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
import kotlin.math.min

@Serializable(with = MainDataSerializer::class)
data class MainData(
    val serverState: ServerState,
    val torrents: List<Torrent>,
    val categories: List<Category>,
    val tags: List<String>,
    val trackers: Map<String, List<String>>
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
        lateinit var serverState: ServerState
        var torrents: Map<String, Torrent>? = null
        var categories: Map<String, Category>? = null
        var tags: List<String>? = null
        var trackers: Map<String, List<String>>? = null

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> serverState = decodeSerializableElement(
                    descriptor,
                    index,
                    ServerState.serializer()
                )
                1 -> torrents = decodeSerializableElement(
                    descriptor,
                    index,
                    MapSerializer(String.serializer(), Torrent.serializer())
                )
                2 -> categories = decodeSerializableElement(
                    descriptor,
                    index,
                    MapSerializer(String.serializer(), Category.serializer())
                )
                3 -> tags = decodeSerializableElement(
                    descriptor,
                    index,
                    ListSerializer(String.serializer())
                )
                4 -> trackers = decodeSerializableElement(
                    descriptor,
                    index,
                    MapSerializer(String.serializer(), ListSerializer(String.serializer()))
                )
            }
        }

        val formattedTrackers = mutableMapOf<String, MutableList<String>>()
        trackers?.forEach { (tracker, hashes) ->
            val formattedTracker = formatUri(tracker)
            val list = formattedTrackers.getOrPut(formattedTracker) { mutableListOf() }
            list.addAll(hashes)
        }

        MainData(
            serverState = serverState,
            torrents = torrents?.mapValues { (hash, torrent) ->
                torrent.copy(hash = hash)
            }?.values?.toList() ?: emptyList(),
            categories = categories?.values?.toList()?.sortedWith(
                Comparator { category1, category2 ->
                    val category1Name = category1.name
                    val category2Name = category2.name

                    for (i in 0..<min(category1Name.length, category2Name.length)) {
                        if (category1Name[i] == '/' && category2Name[i] != '/') {
                            return@Comparator -1
                        } else if (category1Name[i] != '/' && category2Name[i] == '/') {
                            return@Comparator 1
                        } else {
                            val comparison = category1Name[i].toString().compareTo(category2Name[i].toString(), true)
                            if (comparison != 0) {
                                return@Comparator comparison
                            }
                        }
                    }
                    category1Name.length - category2Name.length
                }
            ) ?: emptyList(),
            tags = tags?.sortedWith(String.CASE_INSENSITIVE_ORDER) ?: emptyList(),
            trackers = formattedTrackers
        )
    }
}
