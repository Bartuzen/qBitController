package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import dev.bartuzen.qbitcontroller.model.Category
import dev.bartuzen.qbitcontroller.model.MainData
import dev.bartuzen.qbitcontroller.model.ServerState
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.utils.formatUri
import kotlin.math.min

class MainDataDeserializer : JsonDeserializer<MainData>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): MainData {
        val node = parser.readValueAsTree<JsonNode>()
        val codec = parser.codec

        val serverState = context.readTreeAsValue(node["server_state"], ServerState::class.java)

        val torrents = mutableListOf<Torrent>()
        node["torrents"]?.fields()?.forEach { (hash, torrentNode) ->
            (torrentNode as ObjectNode).put("hash", hash)
            torrents.add(context.readTreeAsValue(torrentNode, Torrent::class.java))
        }

        val categories = node["categories"]?.let { categories ->
            codec.readValue(codec.treeAsTokens(categories), object : TypeReference<Map<String, Category>>() {})
                .values
                .toList()
                .sortedWith(
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
                )
        } ?: emptyList()

        val tags = node["tags"]?.let { tags ->
            codec.readValue(codec.treeAsTokens(tags), object : TypeReference<List<String>>() {})
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
        } ?: emptyList()

        val trackers = node["trackers"]?.let { trackers ->
            codec.readValue(codec.treeAsTokens(trackers), object : TypeReference<Map<String, List<String>>>() {})
        } ?: emptyMap()

        val formattedTrackers = mutableMapOf<String, MutableList<String>>()
        trackers.forEach { (tracker, hashes) ->
            val formattedTracker = formatUri(tracker)
            val list = formattedTrackers.getOrPut(formattedTracker) { mutableListOf() }
            list.addAll(hashes)
        }

        return MainData(
            serverState = serverState,
            torrents = torrents,
            categories = categories,
            tags = tags,
            trackers = formattedTrackers
        )
    }
}
