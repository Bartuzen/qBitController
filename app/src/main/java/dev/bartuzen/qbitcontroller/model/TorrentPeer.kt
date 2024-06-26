package dev.bartuzen.qbitcontroller.model

import dev.bartuzen.qbitcontroller.model.serializers.NullableStringSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class TorrentPeer(
    @SerialName("connection")
    val connection: String,

    @SerialName("country_code")
    val countryCode: String?,

    @SerialName("ip")
    val ip: String,

    @SerialName("port")
    val port: Int,

    @SerialName("flags")
    @Serializable(with = PeerFlagSerializer::class)
    val flags: List<PeerFlag>,

    @SerialName("client")
    @Serializable(with = NullableStringSerializer::class)
    val client: String?,

    @SerialName("peer_id_client")
    @Serializable(with = NullableStringSerializer::class)
    val peerIdClient: String?,

    @SerialName("dl_speed")
    val downloadSpeed: Int,

    @SerialName("up_speed")
    val uploadSpeed: Int,

    @SerialName("downloaded")
    val downloaded: Long,

    @SerialName("uploaded")
    val uploaded: Long,

    @SerialName("progress")
    val progress: Double,

    @SerialName("relevance")
    val relevance: Double,

    @SerialName("files")
    @Serializable(with = PeerFilesSerializer::class)
    val files: List<String> = emptyList()
)

private object PeerFlagSerializer : KSerializer<List<PeerFlag>> {
    override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: List<PeerFlag>) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): List<PeerFlag> {
        return decoder.decodeString().split(" ").mapNotNull { flagStr ->
            PeerFlag.entries.find { it.flag == flagStr }
        }
    }
}

private object PeerFilesSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: List<String>) {
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: Decoder): List<String> {
        return decoder.decodeString().takeIf { it.isNotEmpty() }?.split("\n") ?: emptyList()
    }
}

enum class PeerFlag(val flag: String) {
    INTERESTED_LOCAL_CHOKED_PEER("d"),
    INTERESTED_LOCAL_UNCHOKED_PEER("D"),
    INTERESTED_PEER_CHOKED_LOCAL("u"),
    INTERESTED_PEER_UNCHOKED_LOCAL("U"),
    NOT_INTERESTED_LOCAL_UNCHOKED_PEER("K"),
    NOT_INTERESTED_PEER_UNCHOKED_LOCAL("?"),
    OPTIMISTIC_UNCHOKE("O"),
    PEER_SNUBBED("S"),
    INCOMING_CONNECTION("I"),
    PEER_FROM_DHT("H"),
    PEER_FROM_PEX("X"),
    PEER_FROM_LSD("L"),
    ENCRYPTED_TRAFFIC("E"),
    ENCRYPTED_HANDSHAKE("e"),
    UTP("P")
}
