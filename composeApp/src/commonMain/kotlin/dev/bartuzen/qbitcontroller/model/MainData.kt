package dev.bartuzen.qbitcontroller.model

import dev.bartuzen.qbitcontroller.utils.formatUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = MainDataSerializer::class)
data class MainData(
    val rid: Int,
    val serverState: ServerState,
    val torrents: List<Torrent>,
    val categories: List<Category>,
    val tags: List<String>,
    val trackers: Map<String, List<String>>,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun merge(partialMainData: JsonElement): MainData = withContext(Dispatchers.IO) {
        val mainData = this@MainData
        val partialMainDataMap = partialMainData.jsonObject

        val updatedRid = partialMainDataMap["rid"]?.jsonPrimitive?.int!!
        if (updatedRid < mainData.rid) {
            return@withContext mainData
        }

        val isFullUpdate = partialMainDataMap["full_update"]?.jsonPrimitive?.boolean == true
        if (isFullUpdate) {
            return@withContext json.decodeFromJsonElement(partialMainData)
        }

        val updatedServerState = partialMainDataMap["server_state"]?.jsonObject?.let { partialServerState ->
            val currentServerState = json.encodeToJsonElement(mainData.serverState).jsonObject.toMutableMap()
            currentServerState.putAll(partialServerState)
            json.decodeFromJsonElement(JsonObject(currentServerState))
        } ?: mainData.serverState

        val updatedTorrents = if (
            partialMainDataMap.containsKey("torrents") || partialMainDataMap.containsKey("torrents_removed")
        ) {
            val torrentsMap = mainData.torrents.associateByTo(mutableMapOf()) { it.hash }

            partialMainDataMap["torrents"]?.jsonObject?.let { partialTorrents ->
                partialTorrents.forEach { (hash, partialTorrentJson) ->
                    val existingTorrent = torrentsMap[hash]
                    torrentsMap[hash] = if (existingTorrent != null) {
                        val existingTorrentMap = json.encodeToJsonElement(existingTorrent).jsonObject.toMutableMap()
                        existingTorrentMap.putAll(partialTorrentJson.jsonObject)
                        json.decodeFromJsonElement(JsonObject(existingTorrentMap))
                    } else {
                        val newTorrentMap = partialTorrentJson.jsonObject.toMutableMap()
                        newTorrentMap["hash"] = JsonPrimitive(hash)
                        json.decodeFromJsonElement(JsonObject(newTorrentMap))
                    }
                }
            }

            partialMainDataMap["torrents_removed"]?.jsonArray?.let { removedTorrents ->
                val removedHashes = removedTorrents.mapTo(mutableSetOf()) { it.jsonPrimitive.content }
                removedHashes.forEach { hash ->
                    torrentsMap.remove(hash)
                }
            }

            torrentsMap.values.toList()
        } else {
            mainData.torrents
        }

        val updatedCategories = if (
            partialMainDataMap.containsKey("categories") || partialMainDataMap.containsKey("categories_removed")
        ) {
            val categoriesMap = mainData.categories.associateByTo(mutableMapOf()) { it.name }

            partialMainDataMap["categories"]?.jsonObject?.let { partialCategories ->
                partialCategories.forEach { (categoryName, partialCategoryJson) ->
                    val existingCategory = categoriesMap[categoryName]
                    categoriesMap[categoryName] = if (existingCategory != null) {
                        /**
                         * If the download path is set to the default, it won't be included in the response.
                         * This makes it impossible to know whether the path hasn't changed or has been set to the default.
                         * As a result, we keep showing the old value when the path is set to default.
                         * Oh well, a small sacrifice for performance.
                         * TODO Maybe report this to qBittorrent?
                         */
                        val existingCategoryMap = json.encodeToJsonElement(existingCategory).jsonObject.toMutableMap()
                        existingCategoryMap.putAll(partialCategoryJson.jsonObject)
                        json.decodeFromJsonElement(JsonObject(existingCategoryMap))
                    } else {
                        json.decodeFromJsonElement(partialCategoryJson)
                    }
                }
            }

            partialMainDataMap["categories_removed"]?.jsonArray?.let { removedCategories ->
                val removedNames = removedCategories.mapTo(mutableSetOf()) { it.jsonPrimitive.content }
                removedNames.forEach { name ->
                    categoriesMap.remove(name)
                }
            }

            categoriesMap.values.sortedWith(
                if (updatedServerState.areSubcategoriesEnabled) Category.subcategoryComparator else Category.comparator,
            )
        } else {
            mainData.categories
        }

        val updatedTags = if (
            partialMainDataMap.containsKey("tags") || partialMainDataMap.containsKey("tags_removed")
        ) {
            val tagsSet = mainData.tags.toMutableSet()

            partialMainDataMap["tags"]?.jsonArray?.let { newTags ->
                newTags.forEach { tag ->
                    tagsSet.add(tag.jsonPrimitive.content)
                }
            }

            partialMainDataMap["tags_removed"]?.jsonArray?.let { removedTags ->
                removedTags.forEach { tag ->
                    tagsSet.remove(tag.jsonPrimitive.content)
                }
            }

            tagsSet.sortedWith(String.CASE_INSENSITIVE_ORDER)
        } else {
            mainData.tags
        }

        val updatedTrackers = if (
            partialMainDataMap.containsKey("trackers") || partialMainDataMap.containsKey("trackers_removed")
        ) {
            val trackersMap = mainData.trackers.toMutableMap()

            partialMainDataMap["trackers"]?.jsonObject?.let { partialTrackers ->
                partialTrackers.forEach { (tracker, hashesJson) ->
                    val formattedTracker = formatUri(tracker)
                    trackersMap[formattedTracker] = json.decodeFromJsonElement<List<String>>(hashesJson)
                }
            }

            partialMainDataMap["trackers_removed"]?.jsonArray?.let { removedTrackers ->
                removedTrackers.forEach { tracker ->
                    val formattedTracker = formatUri(tracker.jsonPrimitive.content)
                    trackersMap.remove(formattedTracker)
                }
            }

            trackersMap.toList()
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.first })
                .toMap()
        } else {
            mainData.trackers
        }

        MainData(
            rid = updatedRid,
            serverState = updatedServerState,
            torrents = updatedTorrents,
            categories = updatedCategories,
            tags = updatedTags,
            trackers = updatedTrackers,
        )
    }
}

private object MainDataSerializer : KSerializer<MainData> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MainData") {
        element<ServerState>("server_state")
        element<Map<String, Torrent>>("torrents")
        element<Map<String, Category>>("categories")
        element<List<String>>("tags")
        element<Map<String, List<String>>>("trackers")
        element<Int>("rid")
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
        var decodedRid: Int? = null

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
                5 -> decodedRid = decodeIntElement(descriptor, index)
            }
        }

        val serverState = decodedServerState!!

        val torrents = decodedTorrents?.mapValues { (hash, torrent) ->
            torrent.copy(hash = hash)
        }?.values?.toList() ?: emptyList()

        val categories = decodedCategories?.values?.sortedWith(
            if (serverState.areSubcategoriesEnabled) Category.subcategoryComparator else Category.comparator,
        ) ?: emptyList()

        val tags = decodedTags?.sortedWith(String.CASE_INSENSITIVE_ORDER) ?: emptyList()

        val trackers = mutableMapOf<String, MutableList<String>>()
        decodedTrackers?.forEach { (tracker, hashes) ->
            val formattedTracker = formatUri(tracker)
            val list = trackers.getOrPut(formattedTracker) { mutableListOf() }
            list.addAll(hashes)
        }

        val sortedTrackers = trackers.toList()
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.first })
            .toMap()

        MainData(
            rid = decodedRid!!,
            serverState = serverState,
            torrents = torrents,
            categories = categories,
            tags = tags,
            trackers = sortedTrackers,
        )
    }
}
