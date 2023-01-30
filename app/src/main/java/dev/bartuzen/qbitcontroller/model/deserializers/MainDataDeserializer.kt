package dev.bartuzen.qbitcontroller.model.deserializers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bartuzen.qbitcontroller.model.Category
import dev.bartuzen.qbitcontroller.model.MainData
import dev.bartuzen.qbitcontroller.model.ServerState
import dev.bartuzen.qbitcontroller.model.Torrent

fun parseMainData(mainData: String): MainData {
    val mapper = jacksonObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    val mainDataNode = mapper.readTree(mainData)

    val serverState = mapper.treeToValue(mainDataNode["server_state"], ServerState::class.java)

    val torrents = mainDataNode["torrents"].map { node ->
        (node as ObjectNode).put("hash", node["infohash_v1"].asText())
        mapper.treeToValue(node, Torrent::class.java)
    }

    val categories = mainDataNode["categories"].map { node ->
        Category(
            name = node["name"].asText(),
            savePath = node["savePath"].asText()
        )
    }.sortedBy { it.name.lowercase() }

    val tags = mainDataNode["tags"].map { node ->
        node.asText()
    }.sortedBy { it.lowercase() }

    return MainData(
        serverState = serverState,
        torrents = torrents,
        categories = categories,
        tags = tags
    )
}
