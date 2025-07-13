package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.toMutableStateMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.StringResource

fun <T> stateListSaver() = listSaver<SnapshotStateList<T>, T>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

fun <K, V> stateMapSaver() = listSaver<SnapshotStateMap<K, V>, Pair<K, V>>(
    save = { it.toList() },
    restore = { it.toMutableStateMap() },
)

inline fun <reified T> jsonSaver(json: Json = Json, serializer: KSerializer<T>? = null) = Saver<T, String>(
    save = { if (serializer == null) json.encodeToString(it) else json.encodeToString(serializer, it) },
    restore = { if (serializer == null) json.decodeFromString(it) else json.decodeFromString(serializer, it) },
)

// Can't use Res.allStringResources because of https://github.com/Bartuzen/qBitController/issues/211
fun stringResourceSaver(vararg resources: StringResource) = Saver<StringResource?, String>(
    save = { it?.key },
    restore = { key -> resources.find { it.key == key } },
)
