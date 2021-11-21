package dev.bartuzen.qbitcontroller.ui.common

import androidx.compose.runtime.MutableState
import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PersistentMutableState<T>(
    private val state: SavedStateHandle,
    private val mutableState: MutableState<T>,
    private val key: String
) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val savedValue = state.get<T>(key)
        if (savedValue != null && mutableState.value != savedValue) {
            mutableState.value = savedValue
        }
        return mutableState.value
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        state.set(key, value)
        mutableState.value = value
    }
}