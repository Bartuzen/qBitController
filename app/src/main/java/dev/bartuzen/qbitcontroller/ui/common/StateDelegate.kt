package dev.bartuzen.qbitcontroller.ui.common

import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class StateDelegate<T>(
    private val state: SavedStateHandle,
    private val key: String,
) : ReadWriteProperty<Any?, T?> {
    private var value: T? = null
    private var isUpToDate = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        if (!isUpToDate) {
            isUpToDate = true
            value = state.get<T>(key)
        }
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        this.value = value
        state.set(key, value)
    }
}