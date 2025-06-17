package dev.bartuzen.qbitcontroller.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class JsonPreference<T : Any>(
    private val settings: Settings,
    private val key: String,
    private val initialValue: T,
    private val serializer: KSerializer<T>,
    private val json: Json,
) {
    var value: T
        get() {
            val jsonString = settings[key, ""]
            return if (jsonString.isEmpty()) {
                initialValue
            } else {
                try {
                    json.decodeFromString(serializer, jsonString)
                } catch (_: Exception) {
                    initialValue
                }
            }
        }
        set(value) {
            val jsonString = json.encodeToString(serializer, value)
            settings[key] = jsonString
            flow.value = value
        }

    val flow = MutableStateFlow(value)
}

inline fun <reified T : Any> jsonPreference(
    settings: Settings,
    key: String,
    initialValue: T,
    serializer: KSerializer<T> = serializer<T>(),
    json: Json = Json { ignoreUnknownKeys = true },
) = JsonPreference(
    settings = settings,
    key = key,
    initialValue = initialValue,
    serializer = serializer,
    json = json,
)
