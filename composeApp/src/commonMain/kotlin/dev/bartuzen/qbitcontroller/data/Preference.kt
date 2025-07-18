package dev.bartuzen.qbitcontroller.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class Preference<T : Any>(
    private val settings: Settings,
    private val key: String,
    private val initialValue: T,
    private val type: KClass<T>,
    private val enumValues: Array<T>? = null,
    private val serializer: ((T) -> String)? = null,
    private val deserializer: ((String) -> T)? = null,
) {
    var value: T
        get() {
            if (serializer != null && deserializer != null) {
                val raw = settings[key, serializer(initialValue)]
                return deserializer(raw)
            }

            return when {
                enumValues != null -> enumValues.find {
                    (it as Enum<*>).name == settings[key, (initialValue as Enum<*>).name]
                } ?: initialValue
                type == Int::class -> settings[key, initialValue as Int]
                type == Boolean::class -> settings[key, initialValue as Boolean]
                type == Float::class -> settings[key, initialValue as Float]
                type == Long::class -> settings[key, initialValue as Long]
                type == String::class -> settings[key, initialValue as String]
                else -> throw UnsupportedOperationException("${type.simpleName} is not supported in Preference")
            } as T
        }

        set(value) {
            if (serializer != null && deserializer != null) {
                settings[key] = serializer(value)
            } else {
                when {
                    enumValues != null -> settings[key] = (value as Enum<*>).name
                    type == Int::class -> settings[key] = value as Int
                    type == Boolean::class -> settings[key] = value as Boolean
                    type == Float::class -> settings[key] = value as Float
                    type == Long::class -> settings[key] = value as Long
                    type == String::class -> settings[key] = value as String
                    else -> throw UnsupportedOperationException("${type.simpleName} is not supported in Preference")
                }
            }
            flow.value = value
        }

    val flow = MutableStateFlow(value)
}

inline fun <reified T : Any> preference(
    settings: Settings,
    key: String,
    initialValue: T,
    noinline serializer: ((T) -> String)? = null,
    noinline deserializer: ((String) -> T)? = null,
) = Preference(settings, key, initialValue, T::class, null, serializer, deserializer)

inline fun <reified T : Enum<T>> preference(settings: Settings, key: String, initialValue: T) =
    Preference(settings, key, initialValue, T::class, enumValues<T>())

val preferenceJson = Json {
    ignoreUnknownKeys = true
}

inline fun <reified T : Any> jsonPreference(
    settings: Settings,
    key: String,
    initialValue: T,
    serializer: KSerializer<T> = serializer<T>(),
) = Preference(
    settings = settings,
    key = key,
    initialValue = initialValue,
    type = T::class,
    enumValues = null,
    serializer = { preferenceJson.encodeToString(serializer, it) },
    deserializer = { if (it.isEmpty()) initialValue else preferenceJson.decodeFromString(serializer, it) },
)
