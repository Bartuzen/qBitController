package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import dev.bartuzen.qbitcontroller.model.deserializers.PeerFlagDeserializer

data class TorrentPeer(
    @JsonProperty("connection")
    val connection: String,

    @JsonProperty("country_code")
    val countryCode: String,

    @JsonProperty("ip")
    val ip: String,

    @JsonProperty("port")
    val port: Int,

    @JsonProperty("flags")
    @JsonDeserialize(using = PeerFlagDeserializer::class)
    val flags: List<PeerFlag>
)

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
