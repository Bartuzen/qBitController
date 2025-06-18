package dev.bartuzen.qbitcontroller.utils

sealed interface Platform {
    sealed interface Mobile : Platform {
        data object Android : Mobile
        data object IOS : Mobile
    }

    sealed interface Desktop : Platform {
        data object Windows : Desktop
        data object Linux : Desktop
        data object MacOS : Desktop
    }
}

expect val currentPlatform: Platform
