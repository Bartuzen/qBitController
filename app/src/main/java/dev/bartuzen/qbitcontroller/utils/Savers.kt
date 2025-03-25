package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.toMutableStateMap
import kotlinx.serialization.json.Json

fun <T> stateListSaver() = listSaver<SnapshotStateList<T>, T>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

fun <K, V> stateMapSaver() = listSaver<SnapshotStateMap<K, V>, Pair<K, V>>(
    save = { it.toList() },
    restore = { it.toMutableStateMap() },
)

inline fun <reified T> jsonSaver(json: Json = Json) = Saver<T, String>(
    save = { json.encodeToString(it) },
    restore = { json.decodeFromString(it) },
)
