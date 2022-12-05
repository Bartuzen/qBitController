package dev.bartuzen.qbitcontroller.utils.sharedpreferences

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

class EnumPreference<T : Enum<*>>(
    private val sharedPref: SharedPreferences,
    private val key: String,
    private val initialValue: T,
    private val factory: (String) -> T
) {
    var value: T
        get() = factory(sharedPref.getString(key, initialValue.name)!!)
        set(value) {
            sharedPref.edit()
                .putString(key, value.name)
                .apply()
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

fun <T : Enum<*>> enumPreference(
    sharedPref: SharedPreferences,
    key: String,
    initialValue: T,
    factory: (String) -> T
) = EnumPreference(sharedPref, key, initialValue, factory)
