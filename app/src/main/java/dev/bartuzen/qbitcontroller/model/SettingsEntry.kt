package dev.bartuzen.qbitcontroller.model

data class SettingsEntry<T : Any>(
    val title: String,
    val entryValue: T
)