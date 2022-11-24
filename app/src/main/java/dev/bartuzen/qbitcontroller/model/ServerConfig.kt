package dev.bartuzen.qbitcontroller.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerConfig(
    val id: Int,
    val name: String?,
    val protocol: Protocol,
    val host: String,
    val port: Int,
    val username: String,
    val password: String
) : Parcelable {
    @IgnoredOnParcel
    val protocolString = protocol.toString().lowercase()
}

enum class Protocol {
    HTTP, HTTPS
}
