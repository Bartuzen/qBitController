package dev.bartuzen.qbitcontroller.utils

sealed interface Platform {
    sealed interface Mobile : Platform {
        data object Android : Mobile
        data object IOS : Mobile
    }

    data object Desktop : Platform
}

expect val currentPlatform: Platform
