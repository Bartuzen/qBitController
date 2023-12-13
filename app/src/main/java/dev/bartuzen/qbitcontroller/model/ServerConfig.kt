package dev.bartuzen.qbitcontroller.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ServerConfig(
    val id: Int,
    val name: String?,
    val protocol: Protocol,
    val host: String,
    val port: Int?,
    val path: String?,
    val username: String?,
    val password: String?,
    val trustSelfSignedCertificates: Boolean = false,
    val basicAuth: BasicAuth = BasicAuth(false, null, null)
) : Parcelable {
    val url: String
        get() {
            val url = "${protocol.toString().lowercase()}://$visibleUrl"
            return if (!url.endsWith("/")) {
                "$url/"
            } else {
                url
            }
        }

    val visibleUrl
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
    HTTP,
    HTTPS
}

@Parcelize
@Serializable
data class BasicAuth(
    val isEnabled: Boolean,
    val username: String?,
    val password: String?
) : Parcelable
