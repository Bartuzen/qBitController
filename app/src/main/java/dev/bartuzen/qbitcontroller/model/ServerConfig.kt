package dev.bartuzen.qbitcontroller.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerConfig(
    var id: Int,
    var host: String,
    var username: String,
    var password: String,
    var name: String?
) : Parcelable