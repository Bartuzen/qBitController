package dev.bartuzen.qbitcontroller.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerConfig(
    val id: Int,
    val name: String?,
    val protocol: Protocol,
    val host: String,
    val port: Int?,
    val path: String?,
    val username: String,
    val password: String,
    val trustSelfSignedCertificates: Boolean
) : Parcelable {
    @get:JsonIgnore
    val url: String
        get() {
            val url = "${protocol.toString().lowercase()}://$urlWithoutProtocol"
            return if (!url.endsWith("/")) {
                "$url/"
            } else {
                url
            }
        }

    @get:JsonIgnore
    val urlWithoutProtocol
        get() = buildString {
            append(host)
            port?.let { port ->
                append(":$port")
            }
            path?.let { path ->
                append("/$path")
            }
        }
}

enum class Protocol {
    HTTP, HTTPS
}
