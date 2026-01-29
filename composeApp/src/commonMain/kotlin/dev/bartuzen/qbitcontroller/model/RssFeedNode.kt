package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Serializable(with = RssFeedNodeSerializer::class)
data class RssFeedNode(
    val name: String,
    val feed: RssFeed?,
    val children: MutableList<RssFeedNode>?,
    val path: List<String>,
    val level: Int,
) {
    val isFeed get() = children == null

    val isFolder get() = children != null

    val uniqueId = feed?.uid ?: "$level-$name"
}

object RssFeedNodeSerializer : KSerializer<RssFeedNode> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RssFeedNode")

    override fun serialize(encoder: Encoder, value: RssFeedNode) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): RssFeedNode {
        val element = (decoder as JsonDecoder).decodeJsonElement()

        val feedNode = RssFeedNode("/", null, mutableListOf(), listOf(), 0)
        parseRssFeeds(element, feedNode, 1)
        sortNodes(feedNode)
        return feedNode
    }

    private fun parseRssFeeds(node: JsonElement, feedNode: RssFeedNode, level: Int) {
        for ((key, value) in node.jsonObject) {
            if (isFeed(value)) {
                feedNode.children!!.add(
                    RssFeedNode(
                        name = key,
                        feed = RssFeed(
                            name = key,
                            uid = value.jsonObject["uid"]?.jsonPrimitive?.content ?: "",
                            url = value.jsonObject["url"]?.jsonPrimitive?.content ?: "",
                        ),
                        children = null,
                        path = feedNode.path + key,
                        level = level,
                    ),
                )
            } else {
                val childNode = RssFeedNode(
                    name = key,
                    feed = null,
                    children = mutableListOf(),
                    path = feedNode.path + key,
                    level = level,
                )
                feedNode.children!!.add(childNode)
                parseRssFeeds(value, childNode, level + 1)
            }
        }
    }

    private fun isFeed(node: JsonElement): Boolean {
        for ((key, value) in node.jsonObject) {
            if (key == "uid" && value.jsonPrimitive.isString) {
                return true
            }
        }
        return false
    }

    private fun sortNodes(node: RssFeedNode) {
        node.children?.let { children ->
            children.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            children.forEach { childNode ->
                sortNodes(childNode)
            }
        }
    }
}
