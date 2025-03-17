package dev.bartuzen.qbitcontroller.utils.sharedpreferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class EnumPreference<T : Enum<*>>(
    private val sharedPref: SharedPreferences,
    private val key: String,
    private val initialValue: T,
    private val factory: (String) -> T,
) {
    var value: T
        get() {
            val enumString = sharedPref.getString(key, null)
            return if (enumString != null) factory(enumString) else initialValue
        }
        set(value) {
            sharedPref.edit {
                putString(key, value.name)
            }
            flow.value = value
        }

    @SuppressLint("ApplySharedPref")
    suspend fun setValue(value: T) = withContext(Dispatchers.IO) {
        sharedPref.edit(commit = true) {
            putString(key, value.name)
        }
        flow.value = value
    }

    val flow = MutableStateFlow(value)
}

fun <T : Enum<*>> enumPreference(sharedPref: SharedPreferences, key: String, initialValue: T, factory: (String) -> T) =
    EnumPreference(sharedPref, key, initialValue, factory)
