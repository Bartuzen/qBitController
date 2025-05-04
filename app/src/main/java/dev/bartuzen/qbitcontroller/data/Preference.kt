package dev.bartuzen.qbitcontroller.data

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanStateFlow
import com.russhwolf.settings.coroutines.getFloatStateFlow
import com.russhwolf.settings.coroutines.getIntStateFlow
import com.russhwolf.settings.coroutines.getLongStateFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.coroutines.getStringStateFlow
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class Preference<T : Any>(
    private val settings: ObservableSettings,
    private val key: String,
    private val initialValue: T,
    private val type: KClass<T>,
) {
    var value: T
        get() = when {
            type.java.isEnum -> type.java.enumConstants?.find {
                (it as Enum<*>).name == settings[key, initialValue.toString()]
            } ?: initialValue
            type == Int::class -> settings[key, initialValue as Int]
            type == Boolean::class -> settings[key, initialValue as Boolean]
            type == Float::class -> settings[key, initialValue as Float]
            type == Long::class -> settings[key, initialValue as Long]
            type == String::class -> settings[key, initialValue as String]
            else -> throw UnsupportedOperationException("${type.simpleName} is not supported in Preference")
        } as T
        set(value) = when {
            type.java.isEnum -> settings[key] = (value as Enum<*>).name
            type == Int::class -> settings[key] = value as Int
            type == Boolean::class -> settings[key] = value as Boolean
            type == Float::class -> settings[key] = value as Float
            type == Long::class -> settings[key] = value as Long
            type == String::class -> settings[key] = value as String
            else -> throw UnsupportedOperationException("${type.simpleName} is not supported in Preference")
        }

    val flow = CoroutineScope(Dispatchers.IO).let { scope ->
        when {
            type.java.isEnum -> settings.getStringFlow(key, initialValue.toString())
                .map { type.java.enumConstants?.find { e -> (e as Enum<*>).name == it } ?: initialValue }
                .stateIn(scope, SharingStarted.Eagerly, initialValue)
            type == Int::class -> settings.getIntStateFlow(scope, key, initialValue as Int)
            type == Boolean::class -> settings.getBooleanStateFlow(scope, key, initialValue as Boolean)
            type == Float::class -> settings.getFloatStateFlow(scope, key, initialValue as Float)
            type == Long::class -> settings.getLongStateFlow(scope, key, initialValue as Long)
            type == String::class -> settings.getStringStateFlow(scope, key, initialValue as String)
            else -> throw UnsupportedOperationException("${type.simpleName} is not supported in Preference")
        } as StateFlow<T>
    }
}

inline fun <reified T : Any> preference(settings: ObservableSettings, key: String, initialValue: T) =
    Preference(settings, key, initialValue, T::class)
