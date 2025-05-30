package dev.bartuzen.qbitcontroller.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class Preference<T : Any>(
    private val settings: Settings,
    private val key: String,
    private val initialValue: T,
    private val type: KClass<T>,
    private val enumValues: Array<T>? = null,
) {
    var value: T
        get() = when {
            enumValues != null -> enumValues.find {
                (it as Enum<*>).name == settings[key, initialValue.toString()]
            } ?: initialValue
            type == Int::class -> settings[key, initialValue as Int]
            type == Boolean::class -> settings[key, initialValue as Boolean]
            type == Float::class -> settings[key, initialValue as Float]
            type == Long::class -> settings[key, initialValue as Long]
            type == String::class -> settings[key, initialValue as String]
            else -> throw UnsupportedOperationException("${type.simpleName} is not supported in Preference")
        } as T
        set(value) {
            when {
                enumValues != null -> settings[key] = (value as Enum<*>).name
                type == Int::class -> settings[key] = value as Int
                type == Boolean::class -> settings[key] = value as Boolean
                type == Float::class -> settings[key] = value as Float
                type == Long::class -> settings[key] = value as Long
                type == String::class -> settings[key] = value as String
                else -> throw UnsupportedOperationException("${type.simpleName} is not supported in Preference")
            }
            flow.value = value
        }

    val flow = MutableStateFlow(value)
}

inline fun <reified T : Any> preference(settings: Settings, key: String, initialValue: T) =
    Preference(settings, key, initialValue, T::class)

inline fun <reified T : Enum<T>> preference(settings: Settings, key: String, initialValue: T) =
    Preference(settings, key, initialValue, T::class, enumValues<T>())
