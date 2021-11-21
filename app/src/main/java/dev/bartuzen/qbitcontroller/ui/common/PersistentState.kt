package dev.bartuzen.qbitcontroller.ui.common

import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PersistentState<T>(
    private val state: SavedStateHandle,
    private val key: String,
    private val defaultValue: T
) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
        state.get<T>(key) ?: defaultValue


    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        state.set(key, value)
    }
}