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
    val password: String
) : Parcelable {
    @get:JsonIgnore
    val url
        get() = buildString {
            append("${protocol.toString().lowercase()}://$host")
            port?.let { port ->
                append(":$port")
            }
            path?.let { path ->
                append("/$path")
                if (!path.endsWith("/")) {
                    append("/")
                }
            }
        }
}

enum class Protocol {
    HTTP, HTTPS
}
