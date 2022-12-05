package dev.bartuzen.qbitcontroller.utils.sharedpreferences

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class PrimitivePreference<T : Any>(
    private val sharedPref: SharedPreferences,
    private val key: String,
    private val initialValue: T,
    private val type: KClass<T>
) {
    var value: T
        get() = when (type) {
            Int::class -> {
                sharedPref.getInt(key, initialValue as Int) as T
            }
            Boolean::class -> {
                sharedPref.getBoolean(key, initialValue as Boolean) as T
            }
            Float::class -> {
                sharedPref.getFloat(key, initialValue as Float) as T
            }
            Long::class -> {
                sharedPref.getLong(key, initialValue as Long) as T
            }
            String::class -> {
                sharedPref.getString(key, initialValue as String) as T
            }
            else -> {
                throw UnsupportedOperationException("${type.simpleName} is not supported in PrimitivePreference")
            }
        }
        set(value) {
            val editor = sharedPref.edit()
            when (type) {
                Int::class -> {
                    editor.putInt(key, value as Int) as T
                }
                Boolean::class -> {
                    editor.putBoolean(key, value as Boolean) as T
                }
                Float::class -> {
                    editor.putFloat(key, value as Float) as T
                }
                Long::class -> {
                    editor.putLong(key, value as Long) as T
                }
                String::class -> {
                    editor.putString(key, value as String) as T
                }
                else -> {
                    throw UnsupportedOperationException("${type.simpleName} is not supported in PrimitivePreference")
                }
            }
            editor.apply()
        }

    val flow = MutableStateFlow(value)

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == this.key) {
            flow.value = value
        }
    }

    init {
        sharedPref.registerOnSharedPreferenceChangeListener(listener)
    }
}

inline fun <reified T : Any> primitivePreference(
    sharedPref: SharedPreferences,
    key: String,
    initialValue: T
) = PrimitivePreference(sharedPref, key, initialValue, T::class)
