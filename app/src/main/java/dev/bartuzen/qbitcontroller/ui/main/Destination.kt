package dev.bartuzen.qbitcontroller.ui.main

import kotlinx.serialization.Serializable

sealed class Destination {
    @Serializable
    data object TorrentList : Destination()
}
